package com.example.productapi.service;

import com.example.productapi.dto.OrderDto;
import com.example.productapi.dto.OrderItemDto;
import com.example.productapi.exception.ResourceNotFoundException;
import com.example.productapi.model.Order;
import com.example.productapi.model.OrderItem;
import com.example.productapi.model.Product;
import com.example.productapi.repository.OrderItemRepository;
import com.example.productapi.repository.OrderRepository;
import com.example.productapi.repository.ProductRepository;
import com.example.productapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepo;
    private final OrderItemRepository orderItemRepo;
    private final ProductRepository productRepo;
    private final UserRepository userRepo;

    @Transactional
    public OrderDto create(String username, OrderDto dto) {
        Order order = new Order();
        order.setUser(userRepo.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable : " + username)));
        order = orderRepo.save(order);
        for (OrderItemDto itemDto : dto.items()) {
            Product product = productRepo.findById(itemDto.productId())
                    .orElseThrow(() -> new ResourceNotFoundException("Produit introuvable avec l'id: " + itemDto.productId()));
            OrderItem item = orderItemRepo.save(new OrderItem(null, product, itemDto.quantity(), order));
            order.getItems().add(item);
        }
        return toDto(order);
    }

    @Transactional(readOnly = true)
    public List<OrderDto> findAll() {
        return orderRepo.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<OrderDto> findByUsername(String username) {
        return orderRepo.findByUserUsername(username).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public OrderDto getById(Long id, String username, boolean canSeeAll) {
        Order order = findOrderOrThrow(id);
        // 404 plutôt que 403 : un USER ne doit pas savoir que la commande d'un autre existe
        if (!canSeeAll && (order.getUser() == null || !order.getUser().getUsername().equals(username))) {
            throw new ResourceNotFoundException("Commande introuvable avec l'id: " + id);
        }
        return toDto(order);
    }

    @Transactional
    public void delete(Long id) {
        orderRepo.delete(findOrderOrThrow(id));
    }

    private Order findOrderOrThrow(Long id) {
        return orderRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Commande introuvable avec l'id: " + id));
    }

    private OrderDto toDto(Order order) {
        return new OrderDto(order.getId(), order.getOrderDate(),
                order.getUser() == null ? null : order.getUser().getUsername(),
                order.getItems().stream()
                        .map(i -> new OrderItemDto(i.getId(), i.getProduct().getId(), i.getQuantity()))
                        .toList());
    }
}
