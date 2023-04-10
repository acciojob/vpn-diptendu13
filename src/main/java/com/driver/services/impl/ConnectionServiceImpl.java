package com.driver.services.impl;

import com.driver.model.*;
import com.driver.repository.ConnectionRepository;
import com.driver.repository.ServiceProviderRepository;
import com.driver.repository.UserRepository;
import com.driver.services.ConnectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConnectionServiceImpl implements ConnectionService {
    @Autowired
    UserRepository userRepository2;
    @Autowired
    ServiceProviderRepository serviceProviderRepository2;
    @Autowired
    ConnectionRepository connectionRepository2;

    @Override
    public User connect(int userId, String countryName) throws Exception{
        User user = userRepository2.findById(userId).get();
        if (user.isConnected()){
            throw new Exception("Already connected");
        }
        else if (user.getCountry().getCountryName().name().equalsIgnoreCase(countryName)){
            return user;
        }
        else{
            if (user.getServiceProviderList().isEmpty()){
                throw new Exception("Unable to connect");
            }
            int smallestServiceProviderId = Integer.MAX_VALUE;
            String updatedCountryCode = "";
            for (ServiceProvider serviceProvider : user.getServiceProviderList()){
                for (Country country : serviceProvider.getCountryList()){
                    if (country.getCountryName().name().equalsIgnoreCase(countryName)){
                        updatedCountryCode = country.getCode();
                        if (serviceProvider.getId() < smallestServiceProviderId){
                            smallestServiceProviderId = serviceProvider.getId();
                        }
                    }
                }
            }
            if (smallestServiceProviderId == Integer.MAX_VALUE){
                throw new Exception("Unable to connect");
            }
            else{
                ServiceProvider serviceProvider = serviceProviderRepository2.findById(smallestServiceProviderId).get();
                Connection connection = new Connection();
                user.setMaskedIP(new String(updatedCountryCode + "." + serviceProvider.getId() + "." + userId));
                user.getConnectionList().add(connection);
                user.setConnected(true);
                connection.setUser(user);
                connection.setServiceProvider(serviceProvider);
                serviceProvider.getConnectionList().add(connection);
                serviceProviderRepository2.save(serviceProvider);
            }
        }
        return user;
    }
    @Override
    public User disconnect(int userId) throws Exception {
        User user = userRepository2.findById(userId).get();
        if (!user.isConnected()){
            throw new Exception("Already disconnected");
        }
        String[] credentials = user.getMaskedIP().split("\\.");
        ServiceProvider serviceProvider = serviceProviderRepository2.findById(Integer.parseInt(credentials[1])).get();

        for (Connection connection : user.getConnectionList()){
            if (connection.getUser().equals(user) && connection.getServiceProvider().equals(serviceProvider)){
                user.getConnectionList().remove(connection);
                user.setMaskedIP(null);
                user.setConnected(false);
                serviceProvider.getConnectionList().remove(connection);
                serviceProviderRepository2.save(serviceProvider);
                break;
            }
        }
        return user;
    }
    @Override
    public User communicate(int senderId, int receiverId) throws Exception {
        User sender = userRepository2.findById(senderId).get();
        User receiver = userRepository2.findById(receiverId).get();

        Country currentCountry = receiver.getCountry();  // receiver's original country

        // if receiver is already connected to any vpn, then consider that vpn-country as the receiver's current country
        if (receiver.isConnected()){
            String[] credentials = receiver.getMaskedIP().split("\\.");
            ServiceProvider serviceProvider = serviceProviderRepository2.findById(Integer.parseInt(credentials[1])).get();
            for (Country country : serviceProvider.getCountryList()){
                if (country.getCode().equalsIgnoreCase(credentials[0])){
                    currentCountry = country;
                    break;
                }
            }
        }
        // check if the sender's original country is same as the receiver's current country
        if (sender.getCountry().equals(currentCountry)){
            return sender;
        }
        // if not then connect the sender to suitable vpn
        try{
            sender = connect(senderId, currentCountry.getCountryName().name());
        }
        catch (Exception e){
            throw new Exception("Cannot establish communication");  // any error return this
        }
        return sender;
    }
}
