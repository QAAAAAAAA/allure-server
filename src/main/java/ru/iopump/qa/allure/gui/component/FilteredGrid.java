package ru.iopump.qa.allure.gui.component;

import com.google.common.collect.Maps;
import com.google.common.collect.Lists;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.function.ValueProvider;
import lombok.Getter;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import ru.iopump.qa.util.Str;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static ru.iopump.qa.allure.helper.Util.shortUrl;

public class FilteredGrid<T> {
    private final static String GRID_CLASS = "report-grid";
    private final ListDataProvider<T> sourceDataProvider;
    private final ListDataProvider<T> pageDataProvider;
    @Getter
    private final Grid<T> grid;
    private final VerticalLayout content;
    private final Span pageLabel;
    private final Span pageSummary;
    private final Button firstPageButton;
    private final Button prevPageButton;
    private final Button nextPageButton;
    private final Button lastPageButton;
    private final Map<Col<T>, String> filterValues = Maps.newLinkedHashMap();
    private final List<Col<T>> columnSpecList;
    /** Data columns only (excludes built-in selection column), in spec order. */
    private final List<Grid.Column<T>> dataColumns = Lists.newArrayList();
    private final Map<Grid.Column<T>, Supplier<String>> dynamicFooter = Maps.newHashMap();
    private int pageSize = 50;
    private int currentPage = 0;

    public FilteredGrid(
        @NonNull final ListDataProvider<T> dataProvider,
        @NonNull final List<Col<T>> columnSpecList
    ) {
        this.sourceDataProvider = dataProvider;
        this.pageDataProvider = new ListDataProvider<>(Lists.newArrayList());
        this.grid = new Grid<>();
        this.content = new VerticalLayout();
        this.pageLabel = new Span();
        this.pageSummary = new Span();
        this.firstPageButton = new Button("First", new Icon(VaadinIcon.ANGLE_DOUBLE_LEFT));
        this.prevPageButton = new Button("Prev", new Icon(VaadinIcon.ANGLE_LEFT));
        this.nextPageButton = new Button("Next", new Icon(VaadinIcon.ANGLE_RIGHT));
        this.lastPageButton = new Button("Last", new Icon(VaadinIcon.ANGLE_DOUBLE_RIGHT));
        this.columnSpecList = columnSpecList;

        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COLUMN_BORDERS);
        baseConfigurationGrid();
        filterConfiguration();
        paginationConfiguration();
        configureContent();

        refreshGridData();
        sourceDataProvider.addDataProviderListener(event -> refreshGridData());
    }

    public FilteredGrid<T> addTo(HasComponents parent) {
        parent.add(content);
        return this;
    }

    public FilteredGrid<T> addTo(VerticalLayout parent) {
        parent.add(content);
        parent.setFlexGrow(1, content);
        return this;
    }

    protected Grid.Column<T> addColumn(Col<T> columnSpec) {
        final Grid.Column<T> column;

        switch (columnSpec.getType()) {
            case LINK:
                column = grid.addColumn(link(columnSpec));
                break;
            case NUMBER:
                column = grid.addColumn(text(columnSpec));
                final Supplier<String> footer = () -> {
                    long amount = filteredItems().stream()
                        .mapToLong(item -> Long.parseLong(Str.toStr(columnSpec.getValue().apply(item))))
                        .sum();
                    return "Total: " + amount;
                };
                dynamicFooter.put(column, footer);
                break;
            default:
                column = grid.addColumn(text(columnSpec));
                break;
        }

        column.setKey(columnSpec.getName())
            .setHeader(columnSpec.getName())
            .setAutoWidth(true)
            .setResizable(true)
            .setSortable(columnSpec.isSortable());
        applyColumnSizing(columnSpec, column);
        //noinspection unchecked,rawtypes
        column.setComparator((ValueProvider) columnSpec.getValue());
        dataColumns.add(column);
        return column;
    }

    //region Private methods
    //// PRIVATE ////
    private void filterConfiguration() {
        final HeaderRow filterRow = grid.appendHeaderRow();

        // Selection column is not part of `dataColumns`; filters must align to our spec order.
        if (dataColumns.size() != columnSpecList.size()) {
            throw new IllegalStateException("Column count mismatch: specs=" + columnSpecList.size()
                + ", dataColumns=" + dataColumns.size());
        }

        for (int i = 0; i < columnSpecList.size(); i++) {
            final Col<T> spec = columnSpecList.get(i);
            final Grid.Column<T> column = dataColumns.get(i);
            final HeaderRow.HeaderCell headerCell = filterRow.getCell(column);
            addFilter(spec, headerCell);
        }
    }

    private void addFilter(Col<T> spec, HeaderRow.HeaderCell headerCell) {
        final TextField filterField = new TextField();
        filterField.addValueChangeListener(event -> {
            filterValues.put(spec, event.getValue());
            currentPage = 0;
            applyFilter();
            refreshGridData();
        });
        filterField.setValueChangeMode(ValueChangeMode.LAZY);
        filterField.setValueChangeTimeout(500);
        filterField.setClearButtonVisible(true);
        filterField.setPlaceholder("Filter...");
        filterField.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
        filterField.setWidthFull();
        filterField.getStyle().set("min-width", "0");

        headerCell.setComponent(filterField);
    }

    private void baseConfigurationGrid() {
        grid.addClassName(GRID_CLASS);
        grid.setDataProvider(pageDataProvider);
        grid.removeAllColumns();
        dataColumns.clear();
        grid.setSizeFull();
        grid.setMultiSort(false);
        grid.setSelectionMode(Grid.SelectionMode.MULTI);
        grid.getStyle()
            .set("border-radius", "10px")
            .set("overflow-x", "auto")
            .set("border", "1px solid var(--lumo-contrast-10pct)");
        grid.setAllRowsVisible(false);

        columnSpecList.stream()
            .map(this::addColumn)
            .collect(Collectors.toUnmodifiableList());
    }

    private void paginationConfiguration() {
        firstPageButton.addClickListener(event -> {
            if (currentPage > 0) {
                currentPage = 0;
                refreshGridData();
            }
        });
        prevPageButton.addClickListener(event -> {
            if (currentPage > 0) {
                currentPage--;
                refreshGridData();
            }
        });
        nextPageButton.addClickListener(event -> {
            int totalPages = totalPages(filteredItems().size());
            if (currentPage < totalPages - 1) {
                currentPage++;
                refreshGridData();
            }
        });
        lastPageButton.addClickListener(event -> {
            int totalPages = totalPages(filteredItems().size());
            if (currentPage < totalPages - 1) {
                currentPage = totalPages - 1;
                refreshGridData();
            }
        });

        firstPageButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        prevPageButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        nextPageButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        lastPageButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
    }

    private void configureContent() {
        var pagination = new HorizontalLayout(pageSummary, firstPageButton, prevPageButton, pageLabel, nextPageButton, lastPageButton);
        pagination.setWidthFull();
        pagination.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        pagination.expand(pageSummary);
        pagination.getStyle()
            .set("padding", "8px 4px")
            .set("border-top", "1px solid var(--lumo-contrast-10pct)");

        pageLabel.getStyle()
            .set("font-weight", "600")
            .set("color", "var(--lumo-primary-text-color)");
        pageSummary.getStyle().set("color", "var(--lumo-secondary-text-color)");

        content.setSizeFull();
        content.setPadding(false);
        content.setSpacing(false);
        content.add(grid, pagination);
        content.setFlexGrow(1, grid);
    }

    private Renderer<T> text(Col<T> columnSpec) {
        return new ComponentRenderer<>(row -> {
            var value = Str.toStr(columnSpec.getValue().apply(row));
            var res = new Span(value);
            res.getStyle()
                .set("line-height", "1.4")
                .set("display", "block")
                .set("overflow", "hidden")
                .set("text-overflow", "ellipsis");
            return res;
        });
    }

    private Renderer<T> link(Col<T> columnSpec) {
        return new ComponentRenderer<>(row -> {
            var link = Str.toStr(columnSpec.getValue().apply(row));
            var res = new Anchor(link, StringUtils.defaultIfBlank(shortUrl(link), link));
            res.setTarget("_blank");
            res.getStyle()
                .set("font-weight", "600")
                .set("display", "block")
                .set("overflow", "hidden")
                .set("text-overflow", "ellipsis");
            return res;
        });
    }

    private void updateFooters() {
        dynamicFooter.forEach((col, sup) -> col.setFooter(sup.get()));
    }

    private void applyFilter() {
        sourceDataProvider.setFilter(row -> filterValues.entrySet().stream()
            .filter(entry -> StringUtils.isNotBlank(entry.getValue()))
            .allMatch(entry -> {
                var value = entry.getKey().getValue().apply(row);
                return StringUtils.containsIgnoreCase(Str.toStr(value), entry.getValue());
            }));
    }

    private List<T> filteredItems() {
        return sourceDataProvider.fetch(new Query<>(sourceDataProvider.getFilter()))
            .collect(Collectors.toList());
    }

    private int totalPages(int totalItems) {
        if (totalItems == 0) {
            return 1;
        }
        return (int) Math.ceil(totalItems / (double) pageSize);
    }

    private void refreshGridData() {
        var filtered = filteredItems();
        int totalItems = filtered.size();
        int totalPages = totalPages(totalItems);

        currentPage = Math.max(0, Math.min(currentPage, totalPages - 1));

        int fromIndex = currentPage * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, totalItems);
        List<T> pageItems = totalItems == 0
            ? Lists.newArrayList()
            : filtered.subList(fromIndex, toIndex);

        pageDataProvider.getItems().clear();
        pageDataProvider.getItems().addAll(pageItems);
        pageDataProvider.refreshAll();

        int showingFrom = totalItems == 0 ? 0 : fromIndex + 1;
        int showingTo = totalItems == 0 ? 0 : toIndex;
        prevPageButton.setEnabled(currentPage > 0);
        firstPageButton.setEnabled(currentPage > 0);
        nextPageButton.setEnabled(currentPage < totalPages - 1);
        lastPageButton.setEnabled(currentPage < totalPages - 1);
        pageLabel.setText("Page %d / %d".formatted(currentPage + 1, totalPages));
        pageSummary.setText("Showing %d-%d of %d".formatted(showingFrom, showingTo, totalItems));
        updateFooters();
    }

    private void applyColumnSizing(Col<T> columnSpec, Grid.Column<T> column) {
        // Keep sizing predictable: most columns size to content, link column can take remaining space.
        column.setFlexGrow(0);
        if (columnSpec.getType() == Col.Type.LINK) {
            column.setFlexGrow(1);
            column.setWidth("280px");
        }
    }
//endregion
}
