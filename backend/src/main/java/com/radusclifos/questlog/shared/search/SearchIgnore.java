package com.radusclifos.questlog.shared.search;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Keeps private fields out of search filters and sorts.
 */
@Retention(RUNTIME)
@Target({FIELD, METHOD})
public @interface SearchIgnore {

}
