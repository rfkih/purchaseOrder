package com.id.app.purchaseOrder.services;

import com.id.app.purchaseOrder.dto.UserDto;
import com.id.app.purchaseOrder.entity.User;
import com.id.app.purchaseOrder.mapper.Mapper;
import com.id.app.purchaseOrder.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public List<UserDto> findAll() {
        return userRepository.findAll()
                .stream().map(Mapper::toDto).collect(Collectors.toList());
    }

    public UserDto findById(Integer id) {
        return userRepository.findById(id).map(Mapper::toDto).orElse(null);
    }

    public UserDto create(UserDto dto) {
        User u = Mapper.toEntity(dto);
        u.setCreatedBy("SYSTEM");
        u.setUpdatedBy("SYSTEM");
        return Mapper.toDto(userRepository.save(u));
    }

    public UserDto update(Integer id, UserDto dto) {
        User u = userRepository.findById(id).orElseThrow();
        u.setFirstName(dto.getFirstName());
        u.setLastName(dto.getLastName());
        u.setEmail(dto.getEmail());
        u.setPhone(dto.getPhone());
        u.setUpdatedBy("SYSTEM");
        return Mapper.toDto(userRepository.save(u));
    }

    public void delete(Integer id) {
        userRepository.deleteById(id);
    }
}
