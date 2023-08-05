package com.propagation.User.service.required;

import com.propagation.User.domain.User;
import com.propagation.User.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@RequiredArgsConstructor
@Service
@Slf4j
public class UserServicePropagationRequired {

    private final UserRepository userRepository;

    private final UserServicePropagationRequiredChild userServicePropagationRequiredChild;


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


    @Transactional(propagation = Propagation.REQUIRED)
    public void updateParent(Long id) {
        User user = userRepository.findById(id).orElseThrow(RuntimeException::new);

        user.updateName("parent");
        userServicePropagationRequiredChild.updateChild(id);
    }

    public void updateParentNoTx(Long id) {
        User user = userRepository.findById(id).orElseThrow(RuntimeException::new);

        user.updateName("parent");
        userServicePropagationRequiredChild.updateChild(id);
    }



}
