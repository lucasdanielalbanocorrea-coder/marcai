package io.github.lucasdanielalbanocorreadev.marcai;

import org.springframework.boot.SpringApplication;

public class TestMarcaiApplication {

    public static void main(String[] args) {
        SpringApplication.from(MarcaiApplication::main).with(TestcontainersConfig.class).run(args);
    }

}
