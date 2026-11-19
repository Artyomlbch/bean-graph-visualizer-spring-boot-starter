package org.artyomlbch.beangraphvisualizer.visualizer.core.factory;

import org.artyomlbch.beangraphvisualizer.visualizer.model.filter.Stereotype;
import org.junit.jupiter.api.Test;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.RepositoryDefinition;
import org.springframework.stereotype.Service;

import static org.assertj.core.api.Assertions.assertThat;

// правило для репозиториев Spring Data без @Repository
class StereotypeDetectionTest {

    static class Pet {
    }

    // наследование через CrudRepository, как обычно и пишут
    interface PetRepository extends CrudRepository<Pet, Long> {
    }

    // напрямую от Repository, без готовых методов
    interface PetMarkerRepository extends Repository<Pet, Long> {
    }

    // наследование через свой промежуточный интерфейс
    interface BaseRepository<T> extends CrudRepository<T, Long> {
    }

    interface PetBaseRepository extends BaseRepository<Pet> {
    }

    // @RepositoryDefinition заменяет наследование от Repository
    @RepositoryDefinition(domainClass = Pet.class, idClass = Long.class)
    interface PetDefinedRepository {
        Pet findById(Long id);
    }

    // @RepositoryDefinition на родительском интерфейсе: Spring Data такие интерфейсы тоже считает репозиториями.
    // @Inherited здесь не помогает, в Java он действует только для классов
    @RepositoryDefinition(domainClass = Pet.class, idClass = Long.class)
    interface DefinedBaseRepository {
    }

    interface PetInheritedDefinitionRepository extends DefinedBaseRepository {
    }

    // аннотация есть, правило не нужно, результат тот же
    @org.springframework.stereotype.Repository
    interface AnnotatedPetRepository extends CrudRepository<Pet, Long> {
    }

    // имя похоже на репозиторий, но к Spring Data отношения не имеет
    interface PetLookupRepository {
        Pet find(Long id);
    }

    // явная аннотация важнее правила
    @Service
    static class PetServiceImplementingRepository implements Repository<Pet, Long> {
    }

    @Test
    void repositoryExtendingCrudRepositoryIsRepository() {
        assertThat(GraphFactory.detectStereotype(PetRepository.class)).isEqualTo(Stereotype.REPOSITORY);
    }

    @Test
    void repositoryExtendingMarkerInterfaceIsRepository() {
        assertThat(GraphFactory.detectStereotype(PetMarkerRepository.class)).isEqualTo(Stereotype.REPOSITORY);
    }

    @Test
    void repositoryExtendingOwnBaseInterfaceIsRepository() {
        assertThat(GraphFactory.detectStereotype(PetBaseRepository.class)).isEqualTo(Stereotype.REPOSITORY);
    }

    @Test
    void repositoryWithRepositoryDefinitionIsRepository() {
        assertThat(GraphFactory.detectStereotype(PetDefinedRepository.class)).isEqualTo(Stereotype.REPOSITORY);
    }

    @Test
    void repositoryDefinitionOnParentInterfaceIsFound() {
        assertThat(GraphFactory.detectStereotype(PetInheritedDefinitionRepository.class)).isEqualTo(Stereotype.REPOSITORY);
    }

    @Test
    void annotatedRepositoryIsRepository() {
        assertThat(GraphFactory.detectStereotype(AnnotatedPetRepository.class)).isEqualTo(Stereotype.REPOSITORY);
    }

    @Test
    void nameAloneDoesNotMakeRepository() {
        assertThat(GraphFactory.isSpringDataRepository(PetLookupRepository.class)).isFalse();
        assertThat(GraphFactory.detectStereotype(PetLookupRepository.class)).isEqualTo(Stereotype.UNKNOWN);
    }

    @Test
    void explicitStereotypeWinsOverSpringDataRule() {
        assertThat(GraphFactory.isSpringDataRepository(PetServiceImplementingRepository.class)).isTrue();
        assertThat(GraphFactory.detectStereotype(PetServiceImplementingRepository.class)).isEqualTo(Stereotype.SERVICE);
    }

    @Test
    void plainClassIsNotRepository() {
        assertThat(GraphFactory.detectStereotype(Pet.class)).isEqualTo(Stereotype.UNKNOWN);
    }
}
