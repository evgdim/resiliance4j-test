package com.github.evgdim.restest;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerOpenException;
import io.vavr.CheckedFunction1;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.function.Function;
import java.util.function.Supplier;

public class CircuitBreakerTest
{
    @Test
    public void shouldPassTrough_ThePerson_onSuccessCall() {
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("backendService");

        PersonService personServiceMock = mock(PersonService.class);
        when(personServiceMock.getById(anyLong()))
                .thenReturn(new Person("1"));

        Supplier<Person> personSupplier = CircuitBreaker.decorateSupplier(circuitBreaker, () -> personServiceMock.getById(1L));
        Person person = personSupplier.get();
        assertThat(person.getName()).isEqualTo("1");
    }

    @Test
    public void shouldPassTroughException_onSecondCall() {
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("backendService");

        PersonService personServiceMock = mock(PersonService.class);
        when(personServiceMock.getById(anyLong()))
                .thenReturn(new Person("1"))
                .thenThrow(new RuntimeException("test"));

        Function<Long, Person> getPerson = CircuitBreaker.decorateFunction(circuitBreaker, (Long id) -> personServiceMock.getById(id));
        assertThat(getPerson.apply(1L).getName()).isEqualTo("1");
        assertThatThrownBy(() -> getPerson.apply(2L)).isInstanceOf(RuntimeException.class);
    }

    @Test
    public void shouldOpen_whenServiceFail4Times() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .recordExceptions(RuntimeException.class)
                .ringBufferSizeInClosedState(4)

                .build();
        CircuitBreaker circuitBreaker = CircuitBreaker.of("personService",config);

        PersonService personServiceMock = mock(PersonService.class);
        when(personServiceMock.getById(anyLong()))
                .thenThrow(new RuntimeException("test"))
                .thenThrow(new RuntimeException("test"))
                .thenThrow(new RuntimeException("test"))
                .thenThrow(new RuntimeException("test"))
                .thenReturn(new Person("1"));

        Function<Long, Person> getPerson = CircuitBreaker.decorateFunction(circuitBreaker, (Long id) -> personServiceMock.getById(id));
        assertThatThrownBy(() -> getPerson.apply(2L)).isInstanceOf(RuntimeException.class).hasMessage("test");
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThatThrownBy(() -> getPerson.apply(2L)).isInstanceOf(RuntimeException.class).hasMessage("test");
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThatThrownBy(() -> getPerson.apply(2L)).isInstanceOf(RuntimeException.class).hasMessage("test");
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThatThrownBy(() -> getPerson.apply(2L)).isInstanceOf(RuntimeException.class).hasMessage("test");
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThatThrownBy(() -> getPerson.apply(2L)).isInstanceOf(CallNotPermittedException.class);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }

    @Test
    public void shouldOpen_whenServiceFail4Times_usingSupplier() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .recordExceptions(RuntimeException.class)
                .ringBufferSizeInClosedState(4)

                .build();
        CircuitBreaker circuitBreaker = CircuitBreaker.of("personService",config);

        PersonService personServiceMock = mock(PersonService.class);
        when(personServiceMock.getById(anyLong()))
                .thenThrow(new RuntimeException("test"))
                .thenThrow(new RuntimeException("test"))
                .thenThrow(new RuntimeException("test"))
                .thenThrow(new RuntimeException("test"))
                .thenReturn(new Person("1"));

        Supplier< Person> getPerson1 = CircuitBreaker.decorateSupplier(circuitBreaker, () -> personServiceMock.getById(2L));
        assertThatThrownBy(() -> getPerson1.get()).isInstanceOf(RuntimeException.class).hasMessage("test");
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        Supplier< Person> getPerson2 = CircuitBreaker.decorateSupplier(circuitBreaker, () -> personServiceMock.getById(2L));
        assertThatThrownBy(() -> getPerson2.get()).isInstanceOf(RuntimeException.class).hasMessage("test");
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        Supplier< Person> getPerson3 = CircuitBreaker.decorateSupplier(circuitBreaker, () -> personServiceMock.getById(2L));
        assertThatThrownBy(() -> getPerson3.get()).isInstanceOf(RuntimeException.class).hasMessage("test");
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        Supplier< Person> getPerson4 = CircuitBreaker.decorateSupplier(circuitBreaker, () -> personServiceMock.getById(2L));
        assertThatThrownBy(() -> getPerson4.get()).isInstanceOf(RuntimeException.class).hasMessage("test");
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        Supplier< Person> getPerson5 = CircuitBreaker.decorateSupplier(circuitBreaker, () -> personServiceMock.getById(2L));
        assertThatThrownBy(() -> getPerson5.get()).isInstanceOf(CallNotPermittedException.class);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }
}
