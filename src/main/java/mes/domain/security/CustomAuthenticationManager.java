package mes.domain.security;

import lombok.extern.slf4j.Slf4j;
import mes.domain.entity.User;
import mes.domain.entity.UserGroup;
import mes.domain.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Optional;

@Slf4j
@Component
public class CustomAuthenticationManager implements AuthenticationManager{

	@Autowired
	UserRepository userRepository;

	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {

		String username = authentication.getName();
		String password = authentication.getCredentials().toString();

		Optional<User> optUser = this.userRepository.findByUsername(username);
		if(optUser.isPresent()) {
			User user = optUser.get();
			boolean valid = Pbkdf2Sha256.verification(password, user.getPassword());
			if (valid) {
				// 그룹을  array 에 추가한다
				UserGroup group = user.getUserProfile().getUserGroup();
				SimpleGrantedAuthority authority = new SimpleGrantedAuthority(group.getCode());
				ArrayList<SimpleGrantedAuthority> authorities =  new ArrayList<SimpleGrantedAuthority>();
				authorities.add(authority);
				return new CustomAuthenticationToken(user, password, authorities);
			}
		}else {
			throw new UsernameNotFoundException(username);
		}

		return null;

	}

}

