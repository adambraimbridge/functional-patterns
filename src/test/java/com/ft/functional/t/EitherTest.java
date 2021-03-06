package com.ft.functional.t;

import com.ft.functional.Case;
import com.ft.functional.Either;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

@SuppressWarnings({"ThrowableResultOfMethodCallIgnored", "OptionalGetWithoutIsPresent"})
public class EitherTest {

    @Test
    public void is_either() throws Exception {
        assertThat(Either.left(5).isLeft(), is(true));
        assertThat(Either.left(5).isRight(), is(false));

        assertThat(Either.right(5).isLeft(), is(false));
        assertThat(Either.right(5).isRight(), is(true));
    }

    @Test
    public void is_either_matcher() throws Exception {
        assertThat(Either.left(5).isLeft(is(5)), is(true));
        assertThat(Either.left(5).isLeft(is(10)), is(false));
        assertThat(Either.left(5).isRight(is(5)), is(false));

        assertThat(Either.left(5).isLeft(5), is(true));
        assertThat(Either.left(5).isLeft(10), is(false));
        assertThat(Either.left(5).isRight(5), is(false));

        assertThat(Either.right(5).isLeft(is(5)), is(false));
        assertThat(Either.right(5).isRight(is(5)), is(true));
        assertThat(Either.right(5).isRight(is(10)), is(false));

        assertThat(Either.right(5).isLeft(5), is(false));
        assertThat(Either.right(5).isRight(5), is(true));
        assertThat(Either.right(5).isRight(10), is(false));
    }

    @Test
    public void map_applies_right() throws Exception {
        Either<String, Integer> result =
                Either.<String,Integer>right(5)
                        .map(r -> r*r);

        assertThat(result.isRight(), is(true));
        assertThat(result.right(), is(25));
    }

    @Test
    public void map_passes_left() throws Exception {
        Either<String, Integer> result =
                Either.<String,Integer>left("E")
                .map(r -> r*r);

        assertThat(result.isLeft(), is(true));
        assertThat(result.left(), is("E"));
    }

    @Test
    public void map_can_return_any_right() throws Exception {
        Either<String, String> result =
                Either.<String,Integer>right(5)
                        .map(r -> "R");

        assertThat(result.isRight(), is(true));
        assertThat(result.right(), is("R"));
    }

    @Test
    public void flatMap_applies_right() throws Exception {
        Either<String, Integer> result =
                Either.<String,Integer>right(5)
                        .flatMap(r -> Either.right(r*r));

        assertThat(result.isRight(), is(true));
        assertThat(result.right(), is(25));
    }

    @Test
    public void flatMap_passes_left() throws Exception {
        Either<String, Integer> result =
                Either.<String,Integer>left("E")
                        .flatMap(r -> Either.right(r*r));

        assertThat(result.isLeft(), is(true));
        assertThat(result.left(), is("E"));
    }

    @Test
    public void flatMap_can_return_left() throws Exception {
        Either<String, Integer> result =
                Either.<String,Integer>right(5)
                        .flatMap(r -> Either.left("E"));

        assertThat(result.isLeft(), is(true));
        assertThat(result.left(), is("E"));
    }

    @Test
    public void flatMap_can_return_right_of_any_type() throws Exception {
        Either<String, String> result =
                Either.<String,Integer>right(5)
                        .flatMap(r -> Either.right("R"));

        assertThat(result.isRight(), is(true));
        assertThat(result.right(), is("R"));
    }

    @Test
    public void chain_right_through_different_types_of_result() throws Exception {

        Either<Exception, String> result =
                this.<Exception, Integer>functionThatReturnsRight(10)
                        .map(x -> 2 * x)
                        .flatMap(x -> Either.right(x * 10))
                        .map(this::toDouble)
                        .map(Object::toString);

        assertThat(result.isRight(), is(true));
        assertThat(result.right(), is("200.0"));
    }

    @Test
    public void chain_left_from_initial_left() throws Exception {

        Either<Exception, Double> result =
                this.<Exception, Integer>functionThatReturnsLeft(new RuntimeException("1"))
                        .map(x -> 2 * x)
                        .flatMap(x -> Either.right(x * 10))
                        .map(this::toDouble);

        assertThat(result.isRight(), is(false));
        assertThat(result.isLeft(), is(true));
        assertThat(result.left(), instanceOf(RuntimeException.class));
        assertThat(result.left().getMessage(), is("1"));
    }

    @Test
    public void chain_left_from_flatmap_left() throws Exception {

        Either<Exception, Double> result =
                this.<Exception, Integer>functionThatReturnsRight(10)
                        .map(x -> 2 * x)
                        .flatMap(x -> this.<Exception, Integer>functionThatReturnsLeft(new ArithmeticException("2")))
                        .map(this::toDouble);

        assertThat(result.isRight(), is(false));
        assertThat(result.isLeft(), is(true));
        assertThat(result.left(), instanceOf(RuntimeException.class));
        assertThat(result.left().getMessage(), is("2"));
    }

    @Test
    public void trying_when_no_exception_is_right() throws Exception {
        Either<Exception, Integer> result = Either.trying(() -> 2);

        assertThat(result.isRight(), is(true));
        assertThat(result.right(), is(2));
    }

    @Test
    public void trying_with_exception_is_left() throws Exception {
        Either<Exception, Double> result =
                Either.trying(() -> 1 / 0).map(this::toDouble);

        assertThat(result.isRight(), is(false));
        assertThat(result.isLeft(), is(true));
        assertThat(result.left(), instanceOf(ArithmeticException.class));
    }

    @Test
    public void trying_with_handler() throws Exception {
        Either<String, Integer> right = Either.trying(() -> 23, ex -> Either.left("E"));
        assertThat(right.isRight(), is(true));
        assertThat(right.right(), is(23));

        Either<String, Integer> handleLeft = Either.trying(this::supplierThatAlwaysThrows, ex -> Either.left("E"));
        assertThat(handleLeft.isLeft(), is(true));
        assertThat(handleLeft.left(), is("E"));

        Either<String, Integer> handlerRight = Either.trying(this::supplierThatAlwaysThrows, ex -> Either.right(23));
        assertThat(handlerRight.isRight(), is(true));
        assertThat(handlerRight.right(), is(23));
    }

    @Test
    public void failing_is_left() throws Exception {
        Either<String, Integer> firstLeft = Either.failing(() -> "E1", ex -> "E2");
        assertThat(firstLeft.isLeft(), is(true));
        assertThat(firstLeft.left(), is("E1"));

        Either<String, Integer> secondLeft = Either.failing(this::supplierThatAlwaysThrows, ex -> "E2");
        assertThat(secondLeft.isLeft(), is(true));
        assertThat(secondLeft.left(), is("E2"));
    }

    @Test
    public void map2_applies_function_when_both_right() throws Exception {
        Either<Exception, Integer> result =
                Either.<Exception, Integer>right(42)
                        .map2(Either.right(10), (a, b) -> a * b);

        assertThat(result.isLeft(), is(false));
        assertThat(result.isRight(), is(true));
        assertThat(result.right(), is(420));
    }

    @Test
    public void map2_returns_first_left_when_only_first_either_is_left() throws Exception {
        Either<Exception, Integer> result =
                Either.<Exception, Integer>left(new ArithmeticException())
                        .map2(Either.right(10), (a, b) -> a * b);

        assertThat(result.isRight(), is(false));
        assertThat(result.isLeft(), is(true));
        assertThat(result.left(), instanceOf(ArithmeticException.class));
    }

    @Test
    public void map2_returns_first_left_when_only_second_either_is_left() throws Exception {
        Either<Exception, Integer> result =
                Either.<Exception, Integer>right(42)
                        .map2(Either.<Exception, Integer>left(new ArithmeticException()), (a, b) -> a * b);

        assertThat(result.isRight(), is(false));
        assertThat(result.isLeft(), is(true));
        assertThat(result.left(), instanceOf(ArithmeticException.class));
    }

    @Test
    public void map2_returns_first_left_when_both_are_left() throws Exception {
        Either<Exception, Integer> result =
                Either.<Exception, Integer>left(new ArithmeticException())
                        .map2(Either.<Exception, Integer>left(new NullPointerException()), (a, b) -> a * b);

        assertThat(result.isRight(), is(false));
        assertThat(result.isLeft(), is(true));
        assertThat(result.left(), instanceOf(ArithmeticException.class));
    }

    @Test
    public void mapLeft_can_transform_left_type() throws Exception {
        Either<Exception, Integer> result =
                Either.<String, Integer>left("Error")
                        .mapLeft(ArithmeticException::new);

        assertThat(result.isLeft(), is(true));
        assertThat(result.left(), instanceOf(ArithmeticException.class));
    }

    @Test
    public void mapLeft_passes_right() throws Exception {
        Either<Exception, Integer> result =
                Either.<String, Integer>right(5)
                        .mapLeft(ArithmeticException::new);

        assertThat(result.isRight(), is(true));
        assertThat(result.right(), is(5));
    }

    @Test
    public void flatMapLeft_can_map_to_any_left() throws Exception {
        Either<Exception, Integer> result =
                Either.<String, Integer>left("Error")
                        .flatMapLeft(f -> Either.left(new ArithmeticException(f)));

        assertThat(result.isLeft(), is(true));
        assertThat(result.left(), instanceOf(ArithmeticException.class));
    }

    @Test
    public void flatMapLeft_can_return_right() throws Exception {
        Either<Exception, Integer> result =
                Either.<String, Integer>left("Error")
                        .flatMapLeft(f -> Either.right(2));

        assertThat(result.isRight(), is(true));
        assertThat(result.right(), is(2));
    }

    @Test
    public void flatMapLeft_passes_right() throws Exception {
        Either<Exception, Integer> result =
                Either.<String, Integer>right(5)
                        .flatMapLeft(f -> Either.right(2));

        assertThat(result.isRight(), is(true));
        assertThat(result.right(), is(5));
    }

    @Test
    public void recovers_from_left() throws Exception {
        Either<String, Integer> result =
                Either.<String, Integer>left("Error").recover(__ -> 55);

        assertThat(result.isRight(), is(true));
        assertThat(result.right(), is(55));
    }

    @Test
    public void recover_from_right_is_same_right() throws Exception {
        Either<String, Integer> result =
                Either.<String, Integer>right(42).recover(__ -> 55);

        assertThat(result.isRight(), is(true));
        assertThat(result.right(), is(42));
    }

    @Test
    public void transform_left_can_return_any_right() throws Exception {
        Either<Exception, String> result =
                Either.<String,Integer>left("Error")
                        .transform(either -> Either.right("A"));

        assertThat(result.isRight(), is(true));
        assertThat(result.right(), is("A"));
    }

    @Test
    public void transform_right_can_return_any_left() throws Exception {
        Either<Integer, String> result =
            Either.<String,Integer>right(2)
                    .transform(either -> Either.left(5));

        assertThat(result.isLeft(), is(true));
        assertThat(result.left(), is(5));
    }

    @Test
    public void or_else() throws Exception {
        assertThat(Either.left("L").orElse("A"), is("A"));
        assertThat(Either.right("R").orElse("A"), is("R"));
    }

    @Test
    public void or_else_with_function() throws Exception {
        assertThat(Either.left("L").orElse(__ -> "A"), is("A"));
        assertThat(Either.right("R").orElse(__ -> "A"), is("R"));
    }

    @Test
    public void or_else_throw() throws Exception {
        final Either<String, String> r = Either.right("R");

        assertThat(r.orElseThrow(RuntimeException::new), is("R"));

        try {
            Either.<String, String>left("L").orElseThrow(RuntimeException::new);
            fail("Expecting exception");
        } catch (Exception e) {
            assertThat(e.getMessage(), is("L"));
        }
    }

    @Test
    public void peek_has_no_effect_on_either() throws Exception {
        AtomicInteger integer = new AtomicInteger();

        final Either<String, Object> lefty = Either.left("L");
        assertThat(lefty.peek(either -> integer.incrementAndGet()),is(lefty));
        assertThat(integer.get(), is(1));

        final Either<Object, String> righty = Either.right("R");
        assertThat(righty.peek(either -> integer.incrementAndGet()), is(righty));
        assertThat(integer.get(), is(2));
    }

    @Test
    public void ifLeft() throws Exception {
        AtomicReference<String> ref = new AtomicReference<>("N");

        Either<String,String> righty = Either.right("R");
        Either<String,String> righty1 = righty.ifLeft(ref::set);
        assertThat(ref.get(), is("N"));
        assertThat(righty1, is(righty));

        Either<String, String> lefty = Either.left("L");
        Either<String, String> lefty1 = lefty.ifLeft(ref::set);
        assertThat(ref.get(), is("L"));
        assertThat(lefty1, is(lefty));
    }

    @Test
    public void ifRight() throws Exception {
        AtomicReference<String> ref = new AtomicReference<>("N");

        final Either<String, String> lefty = Either.left("L");
        final Either<String, String> lefty1 = lefty.ifRight(ref::set);
        assertThat(ref.get(), is("N"));
        assertThat(lefty1, is(lefty));

        final Either<String, String> righty = Either.right("R");
        final Either<String, String> righty1 = righty.ifRight(ref::set);
        assertThat(ref.get(), is("R"));
        assertThat(righty1, is(righty));
    }

    @Test
    public void toOptional() throws Exception {
        final Either<Integer,String> left = Either.left(76);
        final Either<Integer,String> right = Either.right("R");

        assertThat(left.toOptional().isPresent(), is(false));
        assertThat(right.toOptional().isPresent(), is(true));
        assertThat(right.toOptional().get(), is("R"));
    }

    @Test
    public void toOptional_transform() throws Exception {
        final Either<Integer,String> left = Either.left(76);
        final Either<Integer,String> right = Either.right("R");

        final Function<String, String> t = r -> r + r;
        assertThat(left.toOptional(t).isPresent(), is(false));
        assertThat(right.toOptional(t).isPresent(), is(true));
        assertThat(right.toOptional(t).get(), is("RR"));
    }

    @Test
    public void fromOptional() throws Exception {
        final Optional<String> nonEmpty = Optional.of("R");
        final Optional<String> empty = Optional.empty();

        assertThat(Either.fromOptional(nonEmpty,"L").isRight("R"), is(true));
        assertThat(Either.fromOptional(empty,"L").isLeft("L"), is(true));

        assertThat(Either.fromOptional(nonEmpty,() -> "L").isRight("R"), is(true));
        assertThat(Either.fromOptional(empty,() -> "L").isLeft("L"), is(true));
    }

    @Test
    public void fold_left() {
        Either<Integer, Integer> left = Either.left(1);

        assertThat(left.fold(i -> i + "A", i -> i + "B"), is("1A"));
    }

    @Test
    public void fold_right() {
        Either<Integer, Integer> right = Either.right(2);

        assertThat(right.fold(i -> i + "A", i -> i + "B"), is("2B"));

    }

    @Test
    public void matchers() throws Exception {
        final Either<Integer,String> left = Either.left(76);
        final Either<Integer,String> right = Either.right("R");


        assertThat(left, is(Either.Matchers.isLeft()));
        assertThat(left, is(Either.Matchers.isLeft(is(76))));
        assertThat(left, is(Either.Matchers.isLeft(76)));
        assertThat(left, not(is(Either.Matchers.isRight())));
        assertThat(left, not(is(Either.Matchers.isLeft(is(77)))));
        assertThat(left, not(is(Either.Matchers.isLeft(77))));
        assertThat(left, not(is(Either.Matchers.isRight(is("R")))));
        assertThat(left, not(is(Either.Matchers.isRight("R"))));

        assertThat(right, is(Either.Matchers.isRight()));
        assertThat(right, not(is(Either.Matchers.isLeft())));
        assertThat(right, is(Either.Matchers.isRight(is("R"))));
        assertThat(right, is(Either.Matchers.isRight("R")));
        assertThat(right, not(is(Either.Matchers.isRight(is("S")))));
        assertThat(right, not(is(Either.Matchers.isRight("S"))));
        assertThat(right, not(Either.Matchers.isLeft(is(76))));
        assertThat(right, not(Either.Matchers.isLeft(76)));
    }

    @Test
    @Ignore
    public void doc_example() {
        final long someInput = System.currentTimeMillis();

        Either<Exception, String> n =
                Either.trying(() -> functionThatMightThrow(someInput))
                        .map(x -> x * 100)
                        .map(x -> String.format("The number of the day is %.2f", x));

        // now handle any exceptions
        if (n.isRight()) {
            // success; value in n.right()
            System.out.println(n.right());
        } else {
            // failure; exception in n.left()
            System.out.println(n.left().getMessage());
        }

        // or using Case
        System.out.println(
                Case.<String>match(n)
                        .when(Either.Matchers.Left).apply(() -> n.left().getMessage())
                        .when(Either.Matchers.isRight(0.0)).apply(() -> "nada")
                        .when(Either.Matchers.isRight(containsString(".0"))).apply(() -> n.right().split(".")[0])
                        .when(Either.Matchers.Right).apply(n::right)
                        .get()
        );
    }

    private Double functionThatMightThrow(long input) throws ArithmeticException {
        final Random random = new Random();
        return input / random.nextDouble();
    }

    private Double toDouble(Number a) {
        return a.doubleValue();
    }

    private <E, R> Either<E, R> functionThatReturnsRight(R rightValue) {
        return Either.right(rightValue);
    }

    private <E, R> Either<E, R> functionThatReturnsLeft(E leftValue) {
        return Either.left(leftValue);
    }

    private <T> T supplierThatAlwaysThrows() {
        throw new RuntimeException("Ex");
    }
}