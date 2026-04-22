package ru.iopump.qa.allure.gui.component;

import com.google.common.collect.Maps;
import com.google.common.collect.Lists;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.button.Button;
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
import java.util.stream.IntStream;

import static ru.iopump.qa.allure.helper.Util.shortUrl;

public class FilteredGrid<T> {
    public static final String FONT_FAMILY = "font-family";
    public static final String GERMANIA_ONE = "Germania One";
    private final static String GRID_CLASS = "report-grid";
    private final ListDataProvider<T> sourceDataProvider;
    private final ListDataProvider<T> pageDataProvider;
    @Getter
    private final Grid<T> grid;
    private final VerticalLayout content;
    private final Span pageLabel;
    private final Button prevPageButton;
    private final Button nextPageButton;
    private final Map<Col<T>, String> filterValues = Maps.newLinkedHashMap();
    private final List<Col<T>> columnSpecList;
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
        this.prevPageButton = new Button("Prev");
        this.nextPageButton = new Button("Next");
        this.columnSpecList = columnSpecList;

        grid.addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_ROW_STRIPES);
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
            .setSortable(columnSpec.isSortable());
        //noinspection unchecked,rawtypes
        column.setComparator((ValueProvider) columnSpec.getValue());
        return column;
    }

    //region Private methods
    //// PRIVATE ////
    private void filterConfiguration() {
        var headerCells = grid.appendHeaderRow().getCells();

        IntStream.range(0, headerCells.size())
            .forEach(index -> {
                final Col<T> spec = columnSpecList.get(index);
                HeaderRow.HeaderCell headerCell = headerCells.get(index);
                addFilter(spec, headerCell);
            });
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
        filterField.setValueChangeTimeout(1000);
        filterField.setClearButtonVisible(true);
        filterField.setPlaceholder("Filter contains ...");

        headerCell.setComponent(filterField);
    }

    private void baseConfigurationGrid() {
        grid.addClassName(GRID_CLASS);
        grid.setDataProvider(pageDataProvider);
        grid.removeAllColumns();
        grid.setSizeFull();
        grid.setSelectionMode(Grid.SelectionMode.MULTI);

        final List<Grid.Column<T>> cols = columnSpecList.stream()
            .map(this::addColumn)
            .collect(Collectors.toUnmodifiableList());
        cols.stream().findFirst()
            .ifPresent(c -> dynamicFooter.put(c, () -> "Count: " + filteredItems().size()));
    }

    private void paginationConfiguration() {
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
    }

    private void configureContent() {
        var pagination = new HorizontalLayout(prevPageButton, nextPageButton, pageLabel);
        pagination.setWidthFull();
        pagination.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        pagination.expand(pageLabel);

        content.setSizeFull();
        content.setPadding(false);
        content.setSpacing(true);
        content.add(grid, pagination);
        content.setFlexGrow(1, grid);
    }

    private Renderer<T> text(Col<T> columnSpec) {
        return new ComponentRenderer<>(row -> {
            var value = Str.toStr(columnSpec.getValue().apply(row));
            var res = new Span(value);
            res.getStyle().set(FONT_FAMILY, GERMANIA_ONE);
            return res;
        });
    }

    private Renderer<T> link(Col<T> columnSpec) {
        return new ComponentRenderer<>(row -> {
            var link = Str.toStr(columnSpec.getValue().apply(row));
            var res = new Anchor(link, StringUtils.defaultIfBlank(shortUrl(link), link));
            res.setTarget("_blank");
            res.getStyle().set(FONT_FAMILY, GERMANIA_ONE);
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

        prevPageButton.setEnabled(currentPage > 0);
        nextPageButton.setEnabled(currentPage < totalPages - 1);
        pageLabel.setText("Page %d / %d, total %d, rows %d".formatted(currentPage + 1, totalPages, totalItems, pageSize));
        updateFooters();
    }
//endregion
}
