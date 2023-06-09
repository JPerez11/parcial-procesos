package com.procesos.parcial.service.impl;

import com.procesos.parcial.dto.ProductRequestDto;
import com.procesos.parcial.exception.NoDataFoundException;
import com.procesos.parcial.exception.ProductAlreadyExistsException;
import com.procesos.parcial.exception.ProductNotBelongUserException;
import com.procesos.parcial.exception.UserNotFoundException;
import com.procesos.parcial.mapper.IProductRequestMapper;
import com.procesos.parcial.model.Product;
import com.procesos.parcial.model.User;
import com.procesos.parcial.repository.IProductRepository;
import com.procesos.parcial.repository.IUserRepository;
import com.procesos.parcial.service.IProductService;
import com.procesos.parcial.util.ExtractAuthorization;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Product service class that implements the methods of the interface.
 * Transactional annotation for requests to the database.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class ProductServiceImpl implements IProductService {

    /**
     * Repository Constructor Injection.
     */
    private final IProductRepository productRepository;
    private final IUserRepository userRepository;
    private final IProductRequestMapper productRequestMapper;

    /**
     * RestTemplate Constructor Injection
     */
    private final RestTemplate restTemplate;

    @Override
    public void createProduct(ProductRequestDto product) {
        if (!userRepository.existsById(product.getUserId())) {
            throw new UserNotFoundException();
        }
        // Product is saved
        productRepository.save(productRequestMapper.toEntity(product));
    }

    @Override
    public void importAllProducts() {
        // The URL is stored in a String
        String url = "https://fakestoreapi.com/products/";
        // A list of the Products is obtained by the RestTemplate query
        Product[] product = restTemplate.getForObject(url, Product[].class);
        // Verify that null is not coming
        if (product == null) {
            throw new NoDataFoundException();
        }
        Long id = ExtractAuthorization.getAuthenticatedUserId();
        User user = userRepository.findUserById(id);
        // The list is converted to an ArrayList
        List<Product> productList =  Arrays.asList(product);
        for (Product prod :
                productList) {
            if (productRepository.existsById(prod.getId())) {
                throw new ProductAlreadyExistsException();
            }
            prod.setUser(user);
        }
        // Products are saved.
        productRepository.saveAll(productList);
    }

    @Override
    public Product createProductById(Long id) {
        // The URL plus the id is stored in a String
        String url = "https://fakestoreapi.com/products/"+id;
        // The Product is obtained by the RestTemplate query
        Product product = restTemplate.getForObject(url, Product.class);
        // Verify that null is not coming
        if (product == null) {
            throw new NoDataFoundException();
        }
        if (productRepository.existsById(product.getId())) {
            throw new ProductAlreadyExistsException();
        }
        Long idUser = ExtractAuthorization.getAuthenticatedUserId();
        User user = userRepository.findUserById(idUser);
        product.setUser(user);
        // Product is saved
        return productRepository.save(product);
    }

    @Override
    public Product getProductById(Long id) {
        // The product is searched
        Product product = productRepository.findProductById(id);
        // Verify that null is not coming
        if (product == null) {
            // If the product is not found it throws an exception
            throw new NoDataFoundException();
        }
        // The Product is returned
        return product;
    }

    @Override
    public List<Product> getAllProducts() {
        // All Products are searched
        List<Product> productList = productRepository.findAll();
        // Verify that the list is not empty
        if (productList.isEmpty()) {
            // exception is thrown
            throw new NoDataFoundException();
        }
        // All Products are returned
        return productList;
    }

    @Override
    public void updateProduct(Long id, ProductRequestDto product) {
        // The product is searched
        Product productToUpdate = productRepository.findById(id).orElseThrow(NoDataFoundException::new);
        if (!Objects.equals(productToUpdate.getUser().getId(), ExtractAuthorization.getAuthenticatedUserId())) {
            throw new ProductNotBelongUserException();
        }

        // The old data is replaced with the customer data
        productToUpdate.setTitle(product.getTitle() );
        productToUpdate.setPrice( product.getPrice() );
        productToUpdate.setDescription( product.getDescription() );
        productToUpdate.setCategory(product.getCategory() );
        productToUpdate.setImage(product.getImage() );

        productRepository.save( productToUpdate );
    }
}
