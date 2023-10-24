package com.propagation.service;

import com.propagation.User.domain.User;
import com.propagation.User.service.UserService;
import com.propagation.User.service.madatory.UserServicePropagationMadatory;
import com.propagation.User.service.nested.UserServicePropagationNested;
import com.propagation.User.service.never.UserServicePropagationNever;
import com.propagation.User.service.not_supported.UserServicePropagationNotSupported;
import com.propagation.User.service.requires_new.UserServicePropagationRequiresNew;
import com.propagation.User.service.required.UserServicePropagationRequired;
import com.propagation.User.service.supports.UserServicePropagationSupports;
import com.propagation.User.util.LogAOP;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.NestedTransactionNotSupportedException;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;


@SpringBootTest
@Import({LogAOP.class})
public class UserServicePropagationTest {

    @Autowired
    UserServicePropagationRequired userServicePropagationRequired;

    @Autowired
    UserServicePropagationSupports userServicePropagationSupports;

    @Autowired
    UserServicePropagationMadatory userServicePropagationMandatory;

    @Autowired
    UserServicePropagationNever userServicePropagationNever;

    @Autowired
    UserServicePropagationNotSupported userServicePropagationNotSupported;

    @Autowired
    UserServicePropagationRequiresNew userServicePropagationRequiresNew;

    @Autowired
    UserServicePropagationNested userServicePropagationNested;

    @Autowired
    UserService userService;

    @BeforeEach
    public void saveUser() {
        userServicePropagationRequired.saveUser();
    }

    @AfterEach
    public void deleteUser() {
        userServicePropagationRequired.deleteUser();
    }

    @Test
    @DisplayName("Transactional_required는 기존 트랜잭션 존재시 기존 트랜잭션을 이용한다.")
    public void Transactional_required() throws InterruptedException {
        userServicePropagationRequired.updateParent(1L);
    }

    @Test
    @DisplayName("Transactional_required는 기존 트랜잭션 존재하지 않으면 새로운 트랜잭션을 생성한다.")
    public void Transactional_required_no_tx() throws InterruptedException {
        userServicePropagationRequired.updateParentNoTx(1L);
    }

    @Test
    @DisplayName("Transactional_Supports는 기존 트랜잭션이 존재하면 기존 트랜잭션을 이용한다.")
    public void Transactional_Supports() throws InterruptedException {
        userServicePropagationSupports.updateParent(1L);

    }

    @Test
    @DisplayName("Transactional_Supports는 기존 트랜잭션이 존재하지 않으면 트랜잭션 없이 수행한다.")
    public void Transactional_Supports_no_tx() throws InterruptedException {
        userServicePropagationSupports.updateParentNoTx(1L);

    }

    @Test
    @DisplayName("Transactional_Mandatory는 기존 트랜잭션이 존재하면 기존 트랜잭션을 이용한다.")
    public void transactional_mandatory() throws InterruptedException {

        userServicePropagationMandatory.updateParent(1L);

    }
    @Test
    @DisplayName("Transactional_Mandatory는 기존 트랜잭션이 존재하지 않으면 exception을 발생시킨다. ")
    public void transactional_mandatory_no_tx() throws InterruptedException {
        assertThrows(IllegalTransactionStateException.class, () -> {
            userServicePropagationMandatory.updateParentNoTx(1L);
        });
    }

    @Test
    @DisplayName("Transactional Propagation Never는 부모 트랜잭션이 있으면 exception을 발생시킨다.")
    public void transactional_never_expect_throw() {
        assertThrows(IllegalTransactionStateException.class, () -> {
            userServicePropagationNever.updateParent(1L);
        });
    }


    @Test
    @DisplayName("Transactional Propagation Never는 부모 트랜잭션이 없어야 exception을 발생시키지 않고 실행된다.")
    public void Transactional_Never() {
        assertDoesNotThrow( () -> {
            userServicePropagationNever.updateParentNoTx(1L);
        });
    }

    @Test
    @DisplayName("Transactional Propagation Not_Supported 기존 트랜잭션이 존재하면 기존 트랜잭션이 걸린 부분만 트랜잭션을 적용하고 Not Supported 가 적용된 부분에는 트랜잭션을 적용하지 않는다.")
    public void transactional_not_supported() throws InterruptedException {
        userServicePropagationNotSupported.updateParent(1L);

    }

    @Test
    @DisplayName("Transactional Propagation Not_Supported 기존 트랜잭션이 존재하지 않을 경우 트랜잭션을 적용하지 않는다.")
    public void transactional_not_supported_non_tx() throws InterruptedException {
        userServicePropagationNotSupported.updateParentNoTx(1L);

    }




    @Test
    @DisplayName("Transactional Propagation Request_New는 트랜잭션이 존재하면 끝날때 까지 대기 후 새로운 트랜잭션을 실행한다.")
    public void Transactional_requires_New() {
        assertDoesNotThrow( () -> {
            userServicePropagationRequiresNew.updateParent(1L);
        });
    }

    @Test
    @DisplayName("Transactional Propagation Request_New는 트랜잭션이 존재지 않으면 새로운 트랜잭션을 생성하고 실행한다.")
    public void Transactional_Request_New_no_tx() {
        assertDoesNotThrow( () -> {
            userServicePropagationRequiresNew.updateParentNoTx(1L);
        });
    }


    @Test
    @DisplayName("Transactional Propagation Nested - db 지원 여부와 jdbc 드라이버에서 지원해주어야 한다. 그렇지 않으면 exception이 발생한다. " +
            "트랜잭션 존재시 REQUIRED와 마친가지로 새 트랜잭션 생성, 아니면 savePoint를 걸로 이후 트랜잭션을 수행한다.( 실패시 save point 까지 롤백되며, savePoint이후는 트랜잭션 처리가 된다.)")
    public void Transactional_Nested() {
        assertThrows(NestedTransactionNotSupportedException.class, () -> {
            userServicePropagationNested.updateParent(1L);
        });
    }

    @Test
    @DisplayName("같은 서비스에서 private 메소드를 호출해도 Transaction이 적용된다.")
    public void Transactional_private() {
        try {
            userService.updateParent(1L);
        }catch (Exception e) {

        }
        User user = userService.findUser(1L);

        assertThat(user.getName()).isEqualTo("save");
    }

}