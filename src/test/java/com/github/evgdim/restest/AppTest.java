package com.github.evgdim.restest;

import static org.mockito.Mockito.*;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.vavr.CheckedFunction1;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.function.Function;
import java.util.function.Supplier;

public class AppTest 
{
    @Test
    public void shouldPassTrough_ThePerson_onSuccessCall() {
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("backendService");

        PersonService personServiceMock = mock(PersonService.class);
        when(personServiceMock.getById(anyLong()))
                .thenReturn(new Person("1"));

        Supplier<Person> personSupplier = CircuitBreaker.decorateSupplier(circuitBreaker, () -> personServiceMock.getById(1L));
        Person person = personSupplier.get();
        Assertions.assertThat(person.getName()).isEqualTo("1");
    }

    @Test
    public void shouldPassTroughException_onSecondCall() {
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("backendService");

        PersonService personServiceMock = mock(PersonService.class);
        when(personServiceMock.getById(anyLong()))
                .thenReturn(new Person("1"))
                .thenThrow(new RuntimeException("test"));

        Function<Long, Person> getPerson = CircuitBreaker.decorateFunction(circuitBreaker, (Long id) -> personServiceMock.getById(id));
        Assertions.assertThat(getPerson.apply(1L).getName()).isEqualTo("1");
        Assertions.assertThatThrownBy(() -> getPerson.apply(2L)).isInstanceOf(RuntimeException.class);
    }

    @Test
    public void shouldPassTroughException() {
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
        System.out.println(circuitBreaker.getState());
        Assertions.assertThatThrownBy(() -> getPerson.apply(2L)).isInstanceOf(RuntimeException.class).withFailMessage("test");
        System.out.println(circuitBreaker.getState());
        Assertions.assertThatThrownBy(() -> getPerson.apply(2L)).isInstanceOf(RuntimeException.class).withFailMessage("test");
        System.out.println(circuitBreaker.getState());
        Assertions.assertThatThrownBy(() -> getPerson.apply(2L)).isInstanceOf(RuntimeException.class).withFailMessage("test");
        System.out.println(circuitBreaker.getState());
        Assertions.assertThatThrownBy(() -> getPerson.apply(2L)).isInstanceOf(RuntimeException.class).withFailMessage("test");
        System.out.println(circuitBreaker.getState());
        Assertions.assertThatThrownBy(() -> getPerson.apply(2L)).isInstanceOf(RuntimeException.class).withFailMessage("test");
        System.out.println(circuitBreaker.getState());
    }
}
