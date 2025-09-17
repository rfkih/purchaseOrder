package com.id.app.purchaseOrder.services;

import com.id.app.purchaseOrder.dto.UserDto;
import com.id.app.purchaseOrder.entity.User;
import com.id.app.purchaseOrder.mapper.Mapper;
import com.id.app.purchaseOrder.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @org.mockito.Mock
    UserRepository userRepository;

    // ---------- helpers ----------
    private static User user(Integer id, String fn, String ln, String email, String phone) {
        User u = new User();
        u.setId(id);
        u.setFirstName(fn);
        u.setLastName(ln);
        u.setEmail(email);
        u.setPhone(phone);
        return u;
    }

    private static UserDto dto(Integer id, String fn, String ln, String email, String phone) {
        UserDto d = new UserDto();
        d.setId(id);
        d.setFirstName(fn);
        d.setLastName(ln);
        d.setEmail(email);
        d.setPhone(phone);
        return d;
    }

    @Test
    void findAll_mapsList() {
        var svc = new UserService(userRepository);
        User u1 = user(1, "A", "One", "a@x.com", "111");
        User u2 = user(2, "B", "Two", "b@x.com", "222");
        when(userRepository.findAll()).thenReturn(List.of(u1, u2));

        UserDto d1 = dto(1, "A", "One", "a@x.com", "111");
        UserDto d2 = dto(2, "B", "Two", "b@x.com", "222");

        try (MockedStatic<Mapper> ms = Mockito.mockStatic(Mapper.class)) {
            ms.when(() -> Mapper.toDto(u1)).thenReturn(d1);
            ms.when(() -> Mapper.toDto(u2)).thenReturn(d2);

            var out = svc.findAll();

            assertThat(out).containsExactly(d1, d2);
            ms.verify(() -> Mapper.toDto(u1));
            ms.verify(() -> Mapper.toDto(u2));
        }
    }

    @Test
    void findById_found_returnsDto() {
        var svc = new UserService(userRepository);
        User u = user(5, "John", "Doe", "j@x.com", "123");
        when(userRepository.findById(5)).thenReturn(Optional.of(u));
        UserDto expected = dto(5, "John", "Doe", "j@x.com", "123");

        try (MockedStatic<Mapper> ms = Mockito.mockStatic(Mapper.class)) {
            ms.when(() -> Mapper.toDto(u)).thenReturn(expected);

            var out = svc.findById(5);

            assertThat(out).isSameAs(expected);
        }
    }

    @Test
    void findById_notFound_returnsNull() {
        var svc = new UserService(userRepository);
        when(userRepository.findById(404)).thenReturn(Optional.empty());

        var out = svc.findById(404);

        assertThat(out).isNull();
    }

    @Test
    void create_mapsEntity_setsAudit_andReturnsDto() {
        var svc = new UserService(userRepository);
        UserDto input = dto(null, "Ana", "Lee", "ana@x.com", "999");

        User mappedEntity = user(null, "Ana", "Lee", "ana@x.com", "999");
        User savedEntity = user(10, "Ana", "Lee", "ana@x.com", "999");
        UserDto mappedDto = dto(10, "Ana", "Lee", "ana@x.com", "999");

        try (MockedStatic<Mapper> ms = Mockito.mockStatic(Mapper.class)) {
            ms.when(() -> Mapper.toEntity(input)).thenReturn(mappedEntity);
            when(userRepository.save(any(User.class))).thenReturn(savedEntity);
            ms.when(() -> Mapper.toDto(savedEntity)).thenReturn(mappedDto);

            var out = svc.create(input);

            // verify audit fields set before save
            verify(userRepository).save(argThat(u ->
                    "SYSTEM".equals(u.getCreatedBy()) &&
                            "SYSTEM".equals(u.getUpdatedBy()) &&
                            "Ana".equals(u.getFirstName())
            ));
            assertThat(out).isSameAs(mappedDto);
        }
    }

    @Test
    void update_happyPath_updatesFields_andReturnsDto() {
        var svc = new UserService(userRepository);
        User existing = user(7, "Old", "Name", "old@x.com", "000");
        when(userRepository.findById(7)).thenReturn(Optional.of(existing));

        UserDto input = dto(null, "New", "Last", "new@x.com", "111");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        UserDto mappedOut = dto(7, "New", "Last", "new@x.com", "111");

        try (MockedStatic<Mapper> ms = Mockito.mockStatic(Mapper.class)) {
            ms.when(() -> Mapper.toDto(any(User.class))).thenReturn(mappedOut);

            var out = svc.update(7, input);

            assertThat(out).isSameAs(mappedOut);
            assertThat(existing.getFirstName()).isEqualTo("New");
            assertThat(existing.getLastName()).isEqualTo("Last");
            assertThat(existing.getEmail()).isEqualTo("new@x.com");
            assertThat(existing.getPhone()).isEqualTo("111");
            assertThat(existing.getUpdatedBy()).isEqualTo("SYSTEM");
        }
    }

    @Test
    void update_notFound_throwsNoSuchElement() {
        var svc = new UserService(userRepository);
        when(userRepository.findById(123)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> svc.update(123, new UserDto()))
                .isInstanceOf(NoSuchElementException.class); // because .orElseThrow() without custom exception
    }

    @Test
    void delete_callsRepository() {
        var svc = new UserService(userRepository);

        svc.delete(55);

        verify(userRepository).deleteById(55);
    }
}
