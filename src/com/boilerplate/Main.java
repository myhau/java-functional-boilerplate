package com.boilerplate;

import rx.Observable;

import java.util.stream.Stream;

public class Main {
    public static Observable<String> getHelloWorldSource() {
        Stream<String> helloWorldStream = Stream.generate(() -> "Hello World");
        return Observable.from(helloWorldStream::iterator);
    }
    public static void main(String[] args) throws Exception {
        getHelloWorldSource().first().subscribe(System.out::println);
    }
}