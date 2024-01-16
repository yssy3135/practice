package org.example;

public class Main {
    public static void main(String[] args) {
        Publisher publisher = new Publisher();

//        publisher.startFlux()
//                .subscribe(System.out::println);

//        publisher.startMono()
//                .subscribe();

        publisher.startMonoEmpty()
                .subscribe();


    }
}