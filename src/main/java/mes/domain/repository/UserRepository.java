package mes.domain.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import mes.domain.entity.User;
import java.util.*;


import javax.transaction.Transactional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

	Optional<User> findByUsername(String username);
	User getUserById(int id);

	void deleteByUsername(String username);


	@Query(value = "SELECT username FROM auth_user WHERE first_name = :firstName AND email = :email", nativeQuery = true)
	List<String> findByFirstNameAndEmailNative(@Param("firstName") String firstName, @Param("email") String email);

//	@Query(value = "SELECT COUNT(*) > 0 FROM auth_user WHERE username = :usernm AND email = :mail", nativeQuery = true)
//	boolean existsByUsernameAndEmail(@Param("usernm") String usernm, @Param("mail") String mail);

	@Query(value = "SELECT CASE WHEN COUNT(*) > 0 THEN 1 ELSE 0 END FROM auth_user WHERE username = :usernm AND email = :mail", nativeQuery = true)
	int existsByUsernameAndEmail(@Param("usernm") String usernm, @Param("mail") String mail);


	@Transactional
	@Modifying
	@Query(value = "update auth_user set password = :pw WHERE username = :userid", nativeQuery = true)
	void PasswordChange(@Param("pw") String pw, @Param("userid") String userid);

	// 사업자 번호와 대표자 이름으로 사용자 검색
	@Query(value = "SELECT username FROM auth_user WHERE first_name = :firstName AND username = :userid1", nativeQuery = true)
	List<String> findByFirstNameAndBusinessNumberNative(@Param("firstName") String firstName, @Param("userid1") String userid1);

	boolean existsByUsername(String username);

	Optional<User> findByPassword(String storedPassword);

	@Query("SELECT u.username FROM User u WHERE u.first_name = :usernm AND u.username = :findUserid")
	List<String> findByFirstNameAndEmail(String usernm, String findUserid);
}
