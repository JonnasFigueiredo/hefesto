package com.hefesto.jira.dto;

/**
 * Referência enxuta de projeto pro dropdown de criação de história.
 *
 * @param key  KEY do projeto (ex: "HEF").
 * @param name nome legível (ex: "hefesto").
 */
public record ProjectRef(String key, String name) {}
