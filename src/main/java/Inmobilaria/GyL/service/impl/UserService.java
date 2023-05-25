package Inmobilaria.GyL.service.impl;

import Inmobilaria.GyL.entity.ImageUser;
import Inmobilaria.GyL.entity.User;
import Inmobilaria.GyL.enums.Role;
import Inmobilaria.GyL.repository.ImageRepository;
import Inmobilaria.GyL.repository.UserRepository;
import Inmobilaria.GyL.service.IImageService;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpSession;
import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final IImageService imageService;

    public UserService(UserRepository userRepository, IImageService imageService) {
        this.userRepository = userRepository;
        this.imageService = imageService;
    }

    @Transactional
    public void createUser(String email, String password, String name, Long dni, String role, MultipartFile icon) throws Exception {

        User user = new User();

        user.setEmail(email);
        user.setPassword(new BCryptPasswordEncoder().encode(password));
        user.setCreateDate(new Date());
        user.setName(name);

        /*user.setRole(Role.valueOf(name));*/
        switch (role) {
            case "cliente":
                user.setRole(Role.CLIENT);
                break;
            case "propietario":
                user.setRole(Role.ENTITY);
                break;
            default:
                user.setRole(Role.CLIENT);
        }
        ImageUser image;
        user.setDni(dni);
        if(icon.getSize() != 0){
            image = imageService.submitImg(icon);
        } else {
            if(role.equals("cliente") ){
                image = imageService.findById("cliente");
            } else {
                image = imageService.findById("propietario");
            }
        }

        user.setIcon(image);
        userRepository.save(user);
    }

    public User getOne(Long id) {
        return userRepository.getOne(id);
    }

    public List<User> listUsers() {
        List<User> users = new ArrayList();
        users = userRepository.findAll();

        return users;
    }

    public void modifyUserPassword(Long id, String password, String newPassword) {

        if (!password.equalsIgnoreCase(newPassword)) {
            Optional<User> response = userRepository.findById(id);
            if (response.isPresent()) {

                User user = response.get();

                user.setPassword(new BCryptPasswordEncoder().encode(newPassword));

                userRepository.save(user);
            }
        }
    }

    @Transactional
    public void modifyUser(Long id, String name, String password, MultipartFile icon) {

        Optional<User> response = userRepository.findById(id);
        if (response.isPresent()) {

            User user = response.get();

            if (new BCryptPasswordEncoder().matches(password, user.getPassword())) {

                user.setName(name);

                String idImage = null;

                if (user.getIcon() != null) {
                    idImage = user.getIcon().getId();
                }

                ImageUser image = imageService.updateImg(icon, idImage);
                user.setIcon(image);

                userRepository.save(user);
            }
        }
    }

    @Transactional
    public void modifyUser(Long id, String name, String password) {

        Optional<User> response = userRepository.findById(id);
        if (response.isPresent()) {

            User user = response.get();

            if (new BCryptPasswordEncoder().matches(password, user.getPassword())) {

                user.setName(name);
                userRepository.save(user);
            }
        }
    }


    /*EntityAdmin Services*/
    @Transactional
    public void adminModifyRole(Long id, String role) {

        Optional<User> response = userRepository.findById(id);

        User user = response.get();

        switch (role) {
            case "cliente":
                user.setRole(Role.CLIENT);
                break;
            case "propietario":
                user.setRole(Role.ENTITY);
                break;
            case "admin":
                user.setRole(Role.ADMIN);
                break;
            default:
                user.setRole(Role.CLIENT);
        }
        userRepository.save(user);
    }

    @Transactional
    public void adminDeleteUser(Long id) {
        userRepository.deleteById(id);
    }

    public List<User> findByName(String word) {
        return userRepository.findByName(word);
    }

    /*End admin*/
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email);

        if (user != null) {

            List<GrantedAuthority> permissions = new ArrayList();

            GrantedAuthority p = new SimpleGrantedAuthority("ROLE_" + user.getRole().toString());

            permissions.add(p);

            ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();

            HttpSession session = attr.getRequest().getSession(true);

            session.setAttribute("userSession", user);

            return new org.springframework.security.core.userdetails.User(user.getName(), user.getPassword(), permissions);

        } else {
            return null;
        }
    }


}