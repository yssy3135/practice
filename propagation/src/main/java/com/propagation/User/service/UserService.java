package com.propagation.User.service;

import com.propagation.User.domain.User;
import com.propagation.User.repository.UserRepository;
import com.propagation.User.service.required.UserServicePropagationRequiredChild;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
@Slf4j
public class UserService {

    private final UserRepository userRepository;


    public void saveUser() {
        userRepository.save(
                User.builder()
                .name("save")
                .build()
        );
    }
    public User findUser(Long id) {
        return userRepository.findById(id).orElseThrow(RuntimeException::new);
    }

    public void deleteUser() {
        User user = userRepository.findById(1L).orElseThrow(RuntimeException::new);
        userRepository.delete(user);
    }


    @Transactional
    public void updateParent(Long id) {
        User user = userRepository.findById(id).orElseThrow(RuntimeException::new);

        user.updateName("update");
        updateParentNoTx(id);
        userRepository.save(user);
    }

    private void updateParentNoTx(Long id) {
        User user = userRepository.findById(id).orElseThrow(RuntimeException::new);

        user.updateName("private");
        userRepository.saveAndFlush(user);
        throw new RuntimeException("exception!!");
    }



}
