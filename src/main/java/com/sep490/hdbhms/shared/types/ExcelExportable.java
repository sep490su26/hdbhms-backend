package com.sep490.hdbhms.shared.types;

import java.util.List;

public interface ExcelExportable<T> {
    List<Object> mapRow(T data);
}
