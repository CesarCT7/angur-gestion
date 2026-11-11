package mx.velian.angurgestion.auth.service.impl;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import mx.velian.angurgestion.auth.dao.UserRepository;
import mx.velian.angurgestion.auth.model.AngurUserDetails;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    public DatabaseUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        return userRepository.findByIdentifier(identifier)
                .map(AngurUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("Credenciales no válidas"));
    }
}
