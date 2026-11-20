package com.example.productapi.service;

import com.example.productapi.dto.CategoryDto;
import com.example.productapi.dto.ProductDto;
import com.example.productapi.exception.ResourceNotFoundException;
import com.example.productapi.model.Category;
import com.example.productapi.repository.CategoryRepository;
import com.example.productapi.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepo;
    private final ProductRepository productRepo;

    @Transactional
    public CategoryDto create(CategoryDto dto) {
        Category category = new Category();
        category.setName(dto.name());
        return toDto(categoryRepo.save(category));
    }

    @Transactional(readOnly = true)
    public CategoryDto getById(Long id) {
        return toDto(findCategoryOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> findAll() {
        return categoryRepo.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductDto> getProducts(Long id) {
        findCategoryOrThrow(id);
        return productRepo.findByCategoryId(id).stream().map(ProductService::toDto).toList();
    }

    @Transactional
    public CategoryDto update(Long id, CategoryDto dto) {
        Category category = findCategoryOrThrow(id);
        category.setName(dto.name());
        return toDto(category);
    }

    @Transactional
    public void delete(Long id) {
        categoryRepo.delete(findCategoryOrThrow(id));
    }

    private Category findCategoryOrThrow(Long id) {
        return categoryRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie introuvable avec l'id: " + id));
    }

    private CategoryDto toDto(Category c) {
        return new CategoryDto(c.getId(), c.getName());
    }
}
