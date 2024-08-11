# Annotations

All Verifyica annotations are defined in a container class `Verifyica`.

[Verifyica.java](api/src/main/java/org/antublue/verifyica/api/Verifyica.java)

---

### @Verifyica.ArgumentSupplier

All test classes must define single method annotated with `@Verifyica.ArgumentSupplier`.

- required
- may return a `Stream, ``Iterable`, `Collection`, arrays, or single `Object`
  - `Stream`, `Iterable`, `Collection`, and arrays can contain mixed `Object` types
- must be public
- must be static
- must not define any parameters
- must return a non-null Object

#### Parallelism

Test argument parallelism (parallel test argument testing) can be defined with an annotation property `parallelism`

- The default `parallelism` value is `1`
- `parallelism` will be constrained to `verifyica.engine.argument.parallelism` 

**Notes**

- If the `@Verifyica.ArgumentSupplier` method returns a `null` object, the test class will be ignored/not reported.

Examples:

```java
@Verifyica.ArgumentSupplier
public static String arguments() {
    return "test";
}
```

```java
@Verifyica.ArgumentSupplier
public static Argument<?> arguments() {
    return Argument.of("test", "test");
}
```

```java
@Verifyica.ArgumentSupplier
public static Collection<String> arguments() {
    Collection<String> collection = new ArrayList<>();
    collection.add("test");
    return collection;
}
```

```java
@Verifyica.ArgumentSupplier
public static Object[] arguments() {
    Object[] objects = new Object[2];
    objects[0] = "foo";
    objects[1] = "bar";
    return objects;
}
```

```java
// Test 2 arguments in parallel/execution submission in order 
@Verifyica.ArgumentSupplier(parallelism = 2)
public static Collection<Argument<String>> arguments() {
    Collection<Argument<String>> collection = new ArrayList<>();

    for (int i = 0; i < 10; i++) {
        collection.add(Argument.ofString("String " + i));
    }

    return collection;
}
```

---

### @Verifyica.Prepare / @Verifyica.Conclude

`@Verifyica.Prepare` methods are executed **once** before a test class is tested.

`@Verifyica.Conclude` methods are executed **once** after a test class is tested.

All methods annotated with `@Verifyica.Prepare` or `@Verifyica.Conclude`:

- optional
- must return `void`
- must be public
- must be static
- must define a single parameter [ClassContext](api/src/main/java/org/antublue/verifyica/api/ClassContext.java)
- may throw `Throwable`

---

### @Verifyica.BeforeAll / @Verifyica.AfterAll

`@Verifyica.BeforeAll` methods are executed **once for each test argument** before test methods.

`@Verifyica.AfterAll` methods are executed **once for each test argument** after test methods;

All methods annotated with `@Verifyica.BeforeAll` or `@Verifyica.AfterAll`:

- optional
- must return `void`
- must be public
- must not be static
- must defined a single parameter [ArgumentContext](api/src/main/java/org/antublue/verifyica/api/ArgumentContext.java)
- may throw `Throwable`

---

### @Verifyica.BeforeEach / @Verifyica.AfterEach

`@Verifyica.BeforeEach` methods are executed **once** before each `@Verifyica.Test` test method.

`@Verifyica.AfterEach` methods are executed **once after each `@Verifyica.Test` test method;

All methods annotated with `@Verifyica.BeforeEach` or `@Verifyica.AfterEach`:

- optional
- must return `void`
- must be public
- must not be static
- must defined a single parameter [ArgumentContext](api/src/main/java/org/antublue/verifyica/api/ArgumentContext.java)
- may throw `Throwable`

---

### @Verifyica.Test

All methods annotated with `@Verifyica.Test`:

- at least 1 `@Verifyica.Test` method is required for concrete classes
- must return `void`
- must be public
- must not be static
- must defined a single parameter [ArgumentContext](api/src/main/java/org/antublue/verifyica/api/ArgumentContext.java)
- may throw `Throwable`

---

### @Verifyica.Order

Used by Verifyica to order test classes / test methods.

- optional

**Notes**

- If `verifyica.engine.class.parallelism` is greater than `1`, orders test class **execution submission order**.
  - Test class execution will still be in parallel.

---

### @Verifyica.DisplayName

Used by Verifyica to set the test class / test method display name.

- optional

**Notes**

- Used for test class / test method ordering if `@Verifyica.Order` is not declared.

---

### @Verifyica.Tag

Repeatable annotation used to tag test classes for filtering.

- optional

---

### @Verifyica.Disabled

Indicates the Verifyica that the test class / test method is disabled/do not test.

- optional

---

### @Verifyica.ClassInterceptorSupplier

Used to register a test class specific [ClassInterceptor](api/src/main/java/org/antublue/verifyica/api/interceptor/ClassInterceptor.java)

- optional
- may return a `Stream`, `Iterable`, `Collection`, arrays, or single `ClassInterceptor` instance
- must be public
- must be static
- must not define any parameters
- may throw `Throwable`