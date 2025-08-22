package com.assignment.fileextension.repository;

import com.assignment.fileextension.entity.CustomExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@DisplayName("CustomExtensionRepository 테스트")
class CustomExtensionRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CustomExtensionRepository repository;

    private CustomExtension zipExtension;
    private CustomExtension rarExtension;

    @BeforeEach
    void setUp() {
        zipExtension = CustomExtension.builder()
                .extension("zip")
                .build();

        rarExtension = CustomExtension.builder()
                .extension("rar")
                .build();

        entityManager.persistAndFlush(zipExtension);
        entityManager.persistAndFlush(rarExtension);
    }

    @Test
    @DisplayName("확장자명으로 찾기")
    void findByExtension() {
        // when
        Optional<CustomExtension> found = repository.findByExtension("zip");

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getExtension()).isEqualTo("zip");
    }

    @Test
    @DisplayName("존재하지 않는 확장자명으로 찾기")
    void findByExtension_NotFound() {
        // when
        Optional<CustomExtension> found = repository.findByExtension("nonexistent");

        // then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("확장자 존재 여부 확인 - 존재하는 경우")
    void existsByExtension_True() {
        // when
        boolean exists = repository.existsByExtension("zip");

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("확장자 존재 여부 확인 - 존재하지 않는 경우")
    void existsByExtension_False() {
        // when
        boolean exists = repository.existsByExtension("nonexistent");

        // then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("커스텀 확장자 개수 조회")
    void countCustomExtensions() {
        // when
        long count = repository.countCustomExtensions();

        // then
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("빈 테이블에서 커스텀 확장자 개수 조회")
    void countCustomExtensions_Empty() {
        // given - 모든 데이터 삭제
        repository.deleteAll();

        // when
        long count = repository.countCustomExtensions();

        // then
        assertThat(count).isEqualTo(0);
    }

    @Test
    @DisplayName("모든 확장자명 조회")
    void findAllExtensions() {
        // when
        List<String> extensions = repository.findAllExtensions();

        // then
        assertThat(extensions).hasSize(2);
        assertThat(extensions).containsExactlyInAnyOrder("zip", "rar");
    }

    @Test
    @DisplayName("빈 테이블에서 모든 확장자명 조회")
    void findAllExtensions_Empty() {
        // given - 모든 데이터 삭제
        repository.deleteAll();

        // when
        List<String> extensions = repository.findAllExtensions();

        // then
        assertThat(extensions).isEmpty();
    }

    @Test
    @DisplayName("중복된 확장자 저장 시 예외 발생")
    void saveDuplicateExtension_ThrowsException() {
        // given
        CustomExtension duplicate = CustomExtension.builder()
                .extension("zip") // 이미 존재하는 확장자
                .build();

        // when & then
        assertThatThrownBy(() -> {
            entityManager.persistAndFlush(duplicate);
        }).isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("확장자 삭제")
    void deleteExtension() {
        // given
        Long zipId = zipExtension.getId();

        // when
        repository.deleteById(zipId);
        entityManager.flush();

        // then
        Optional<CustomExtension> found = repository.findById(zipId);
        assertThat(found).isEmpty();

        // 남은 확장자 개수 확인
        long count = repository.countCustomExtensions();
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("새 확장자 저장")
    void saveNewExtension() {
        // given
        CustomExtension newExtension = CustomExtension.builder()
                .extension("pdf")
                .build();

        // when
        CustomExtension saved = repository.save(newExtension);
        entityManager.flush();

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getExtension()).isEqualTo("pdf");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();

        // 전체 개수 확인
        long count = repository.countCustomExtensions();
        assertThat(count).isEqualTo(3);
    }

    @Test
    @DisplayName("최대 길이 제한 테스트 (20자)")
    void saveMaxLengthExtension() {
        // given - 20자 확장자
        String maxLengthExtension = "a".repeat(20);
        CustomExtension extension = CustomExtension.builder()
                .extension(maxLengthExtension)
                .build();

        // when
        CustomExtension saved = repository.save(extension);
        entityManager.flush();

        // then
        assertThat(saved.getExtension()).isEqualTo(maxLengthExtension);
        assertThat(saved.getExtension()).hasSize(20);
    }
}