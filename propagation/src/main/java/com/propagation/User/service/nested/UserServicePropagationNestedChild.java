package com.propagation.User.service.nested;

import com.propagation.User.domain.User;
import com.propagation.User.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
@Slf4j
public class UserServicePropagationNestedChild {

    private final UserRepository userRepository;

    public void saveUser() {
        userRepository.save(
                User.builder()
                .name("save")
                .build()
        );
    }

    public void deleteUser() {
        User user = userRepository.findById(1L).orElseThrow(RuntimeException::new);
        userRepository.delete(user);
    }

    @Transactional(propagation = Propagation.NESTED)
    public void updateChild(Long id) {
        User user = userRepository.findById(id).orElseThrow(RuntimeException::new);
        user.updateName("child");

    }
    public void updateChildNoTx(Long id) {
        User user = userRepository.findById(id).orElseThrow(RuntimeException::new);
        user.updateName("child");
    }


}
