package com.github.evgdim.restest;

public class PersonServiceImpl implements PersonService{

    @Override
    public Person getById(Long id) {
        return new Person("test");
    }
}
