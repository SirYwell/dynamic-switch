# Dynamic Switch

Making use of [`MethodHandles#tableSwitch`](https://docs.oracle.com/en/java/javase/19/docs/api/java.base/java/lang/invoke/MethodHandles.html#tableSwitch(java.lang.invoke.MethodHandle,java.lang.invoke.MethodHandle...))
to add `switch` support to your favorite class!

**Note**: The code from this project is not meant to be used productively.

## Example

```java
class DynamicSwitchExample {

  public static void main(String[] args) {
    MyUnique a = new MyUnique("!");
    MyUnique b = new MyUnique("You'll never read this");
    MyUnique c = new MyUnique("Lorem ipsum");
    DynamicSwitch<MyUnique, String> switch_ = DynamicSwitch.builder(MyUnique::id, MyUnique.class, String.class)
        .case_(b, () -> "Hello World")
        .case_(c, MyUnique::identifier)
        .build(v -> "=====" + v.identifier() + "=====");

    System.out.println(switch_.invoke(a));
    System.out.println(switch_.invoke(b));
    System.out.println(switch_.invoke(c));
    System.out.println(switch_.invoke(new MyUnique("?")));
  }

  static class MyUnique {
    private static int counter;
    private final int id;
    private final String identifier;

    MyUnique(String identifier) {
      this.identifier = identifier;
      this.id = counter++;
    }

    public int id() {
      return id;
    }

    public String identifier() {
      return identifier;
    }
  }
}
```

Output:
```
=====!=====
Hello World
Lorem ipsum
=====?=====
```
