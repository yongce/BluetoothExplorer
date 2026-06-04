package androidx.annotation

@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.CLASS,
    AnnotationTarget.CONSTRUCTOR
)
@Retention(AnnotationRetention.BINARY)
annotation class MainThread
