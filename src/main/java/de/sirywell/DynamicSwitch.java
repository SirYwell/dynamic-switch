package de.sirywell;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

import static java.lang.invoke.MethodHandles.dropArguments;
import static java.lang.invoke.MethodHandles.empty;
import static java.lang.invoke.MethodHandles.tableSwitch;
import static java.lang.invoke.MethodType.methodType;

public final class DynamicSwitch<T, R> {
  private final ToIntFunction<T> intExtractor;
  private final MethodHandle tableSwitch;

  DynamicSwitch(ToIntFunction<T> intExtractor, MethodHandle defaultCase, MethodHandle[] cases) {
    this.intExtractor = intExtractor;
    this.tableSwitch = tableSwitch(defaultCase, cases);
  }

  public R invoke(T t) {
    int i = intExtractor.applyAsInt(t);
    try {
      @SuppressWarnings("unchecked")
      R result = (R) tableSwitch.invoke(i, t);
      return result;
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }

  public static <T, R> DynamicSwitchBuilder<T, R> builder(ToIntFunction<T> idExtractor, Class<T> inputType, Class<R> outputType) {
    return new DynamicSwitchBuilder<>(idExtractor, inputType, outputType);
  }

  static class DynamicSwitchBuilder<T, R> {
    private static final MethodHandle SUPPLIER_METHOD;
    private static final MethodHandle FUNCTION_METHOD;

    static {
      MethodHandles.Lookup lookup = MethodHandles.lookup();
      MethodHandle tmpSupplier;
      MethodHandle tmpFunction;
      try {
        tmpSupplier = lookup.findVirtual(Supplier.class, "get", methodType(Object.class));
        tmpFunction = lookup.findVirtual(Function.class, "apply", methodType(Object.class, Object.class));
      } catch (NoSuchMethodException | IllegalAccessException e) {
        throw new RuntimeException(e);
      }
      SUPPLIER_METHOD = tmpSupplier;
      FUNCTION_METHOD = tmpFunction;
    }

    private final ToIntFunction<T> idExtractor;
    private final Class<T> inputType;
    private final Class<R> outputType;
    private final List<Integer> ids = new ArrayList<>();
    private final List<MethodHandle> handles = new ArrayList<>();
    private MethodHandle defaultCase;

    DynamicSwitchBuilder(ToIntFunction<T> idExtractor, Class<T> inputType, Class<R> outputType) {
      this.idExtractor = idExtractor;
      this.inputType = inputType;
      this.outputType = outputType;
    }

    public DynamicSwitchBuilder<T, R> case_(T constant, Supplier<R> run) {
      ids.add(idExtractor.applyAsInt(constant));
      handles.add(createForSupplier(run));
      return this;
    }
    public DynamicSwitchBuilder<T, R> case_(T constant, Function<T, R> run) {
      ids.add(idExtractor.applyAsInt(constant));
      handles.add(createForFunction(run));
      return this;
    }

    public DynamicSwitch<T, R> build(Supplier<R> defaultCase) {
      this.defaultCase = createForSupplier(defaultCase);
      return build();
    }
    public DynamicSwitch<T, R> build(Function<T ,R> defaultCase) {
      this.defaultCase = createForFunction(defaultCase);
      return build();
    }

    public DynamicSwitch<T, R> build() {
      int size = ids.stream().mapToInt(Integer::intValue).max().orElse(0);
      int[] cases = new int[size + 1];
      Arrays.fill(cases, -1);
      for (int i = 0; i < ids.size(); i++) {
        cases[ids.get(i)] = i;
      }
      if (defaultCase == null) {
        defaultCase = empty(methodType(outputType, int.class));
      }
      return new DynamicSwitch<>(
          t -> {
            int i = idExtractor.applyAsInt(t);
            if (i < 0 || i > size) {
              return -1;
            }
            return cases[i];
          },
          defaultCase,
          handles.toArray(MethodHandle[]::new)
      );
    }
    private MethodHandle createForSupplier(Supplier<R> supplier) {
      MethodHandle adapted = SUPPLIER_METHOD.bindTo(supplier).asType(methodType(outputType));
      return dropArguments(adapted, 0, int.class, inputType);
    }
    private MethodHandle createForFunction(Function<T, R> function) {
      MethodHandle adapted = FUNCTION_METHOD.bindTo(function).asType(methodType(outputType, inputType));
      return dropArguments(adapted, 0, int.class);
    }
  }
}
