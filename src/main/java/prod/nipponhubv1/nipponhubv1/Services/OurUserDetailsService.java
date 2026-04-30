package prod.nipponhubv1.nipponhubv1.Services;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import prod.nipponhubv1.nipponhubv1.Models.OurUser;
import prod.nipponhubv1.nipponhubv1.Repository.UserRepository;

/**
 * Implémentation de UserDetailsService pour Spring Security.
 * Charge OurUser par email depuis PostgreSQL.
 * Le compte bloqué (active=false) lève un DisabledException automatiquement
 * grâce à isEnabled() sur OurUser.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OurUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
            .filter(OurUser::isActive)
            .orElseThrow(() ->
                new UsernameNotFoundException("Aucun utilisateur actif pour : " + email)
            );
    }
}
