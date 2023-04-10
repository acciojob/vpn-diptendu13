package com.driver.services.impl;

import com.driver.model.Country;
import com.driver.model.CountryName;
import com.driver.model.ServiceProvider;
import com.driver.model.User;
import com.driver.repository.CountryRepository;
import com.driver.repository.ServiceProviderRepository;
import com.driver.repository.UserRepository;
import com.driver.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    UserRepository userRepository3;
    @Autowired
    ServiceProviderRepository serviceProviderRepository3;
    @Autowired
    CountryRepository countryRepository3;

    @Override
    public User register(String username, String password, String countryName) throws Exception{
        User user = new User();
        Country country = new Country();
        for (CountryName countryName1 : CountryName.values()){
            if (countryName1.name().equalsIgnoreCase(countryName)){
                country.setCountryName(countryName1);
                country.setCode(countryName1.toCode());
                country.setServiceProvider(null);
                country.setUser(user);
                break;
            }
        }
        user.setUsername(username);
        user.setPassword(password);
        user.setCountry(country);
        user.setMaskedIP(null);
        user.setConnected(false);
        userRepository3.save(user); // this is to get the user id assigned
        user.setOriginalIP(new String(user.getCountry().getCode()+"."+user.getId()));
        userRepository3.save(user);

        return user;
    }

    @Override
    public User subscribe(Integer userId, Integer serviceProviderId) {
        User user = userRepository3.findById(userId).get();

        ServiceProvider serviceProvider = serviceProviderRepository3.findById(serviceProviderId).get();

        serviceProvider.getUsers().add(user);
        user.getServiceProviderList().add(serviceProvider);

        serviceProviderRepository3.save(serviceProvider);

        return user;
    }
}
