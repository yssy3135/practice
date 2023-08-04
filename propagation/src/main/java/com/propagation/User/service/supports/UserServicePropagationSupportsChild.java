package com.propagation.User.service.supports;

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
public class UserServicePropagationSupportsChild {

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

    @Transactional(propagation = Propagation.SUPPORTS)
    public void updateChild(Long id) {
        log.info("call child tx");
        log.info("child tx name = {}", TransactionSynchronizationManager.getCurrentTransactionName());
        User user = userRepository.findById(id).orElseThrow(RuntimeException::new);
        user.updateName("child");

        log.info("child tx end");
    }
    public void updateChildNoTx(Long id) {
        log.info("call child tx");
        log.info("child tx name = {}", TransactionSynchronizationManager.getCurrentTransactionName());
        User user = userRepository.findById(id).orElseThrow(RuntimeException::new);
        user.updateName("child");

        log.info("child tx end");
    }


}
