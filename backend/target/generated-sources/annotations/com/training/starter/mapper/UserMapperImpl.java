package com.training.starter.mapper;

import com.training.starter.dto.request.CreateUserRequest;
import com.training.starter.dto.request.UpdateUserRequest;
import com.training.starter.dto.response.UserResponse;
import com.training.starter.entity.User;
import java.time.LocalDateTime;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-07-19T21:55:47+0700",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 22.0.2 (Oracle Corporation)"
)
@Component
public class UserMapperImpl implements UserMapper {

    @Override
    public UserResponse toResponse(User user) {
        if ( user == null ) {
            return null;
        }

        Long id = null;
        String username = null;
        String email = null;
        String fullName = null;
        boolean active = false;
        LocalDateTime createdAt = null;

        id = user.getId();
        username = user.getUsername();
        email = user.getEmail();
        fullName = user.getFullName();
        active = user.isActive();
        createdAt = user.getCreatedAt();

        String role = user.getRole().name();

        UserResponse userResponse = new UserResponse( id, username, email, fullName, role, active, createdAt );

        return userResponse;
    }

    @Override
    public User toEntity(CreateUserRequest request) {
        if ( request == null ) {
            return null;
        }

        User.UserBuilder user = User.builder();

        user.username( request.username() );
        user.email( request.email() );
        user.password( request.password() );
        user.fullName( request.fullName() );

        return user.build();
    }

    @Override
    public void updateEntity(User user, UpdateUserRequest request) {
        if ( request == null ) {
            return;
        }

        if ( request.email() != null ) {
            user.setEmail( request.email() );
        }
        if ( request.fullName() != null ) {
            user.setFullName( request.fullName() );
        }
        if ( request.active() != null ) {
            user.setActive( request.active() );
        }
    }
}
