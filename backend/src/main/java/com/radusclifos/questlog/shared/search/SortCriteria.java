package com.radusclifos.questlog.shared.search;

import org.springframework.data.domain.Sort;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SortCriteria {

    @NotBlank
    private String field;

    @NotNull
    private Sort.Direction direction;

    /**
     * Optional enum values in ascending priority order
     * Omitted values follow them
     * */
    private List<String> customOrder;

    /** Places null values last in both ASC and DESC; false keeps database defaults. */
    private boolean nullsLast;

    public SortCriteria(String field, Sort.Direction direction, List<String> customOrder) {
        this(field, direction, customOrder, false);
    }

    public SortCriteria(String field, Sort.Direction direction) {
        this.field = field;
        this.direction = direction;
    }

}
