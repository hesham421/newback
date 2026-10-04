package com.acme.widget;

import org.springframework.data.jpa.repository.JpaRepository;

/** Test fixture: a repository of a consuming application outside the core packages. */
public interface WidgetRepository extends JpaRepository<Widget, Long> {
}
