package cn.ykccchen.businessutil.match;

final class DuplicateKeyDiagnostics {

    private DuplicateKeyDiagnostics() {
    }

    static String safeValue(Object value) {
        try {
            return String.valueOf(value);
        } catch (RuntimeException exception) {
            String valueType = value == null ? "null" : value.getClass().getName();
            return "<toString-failed:valueType=" + valueType
                    + ",exceptionType=" + exception.getClass().getName() + ">";
        }
    }
}
