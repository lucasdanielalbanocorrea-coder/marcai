package io.github.lucasdanielalbanocorreadev.marcai;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfig.class)
@SpringBootTest
class MarcaiApplicationTests {

    @Test
    void contextLoads() {
    }

}
