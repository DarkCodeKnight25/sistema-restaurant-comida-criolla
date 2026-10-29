package pe.dvega.restaurant.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import pe.dvega.restaurant.dominio.*;
import pe.dvega.restaurant.repositorio.*;
import pe.dvega.restaurant.servicio.Eventos;
import pe.dvega.restaurant.web.Dtos.*;

/** Gestión de la carta. Marcar agotado lo puede hacer cocina, mozo o caja; editar platos, solo administración. */
@RestController
@RequestMapping("/api/carta")
public class CartaController {

    private final PlatoRepositorio platos;
    private final CategoriaRepositorio categorias;
    private final EstacionRepositorio estaciones;
    private final Eventos eventos;

    public CartaController(PlatoRepositorio platos, CategoriaRepositorio categorias, EstacionRepositorio estaciones,
                           Eventos eventos) {
        this.platos = platos;
        this.categorias = categorias;
        this.estaciones = estaciones;
        this.eventos = eventos;
    }

    /** Toda la carta, incluidos los platos retirados. */
    @GetMapping
    @Transactional(readOnly = true)
    CartaDto todo() {
        return new CartaDto(estaciones.findAllByOrderByOrden().stream().map(EstacionDto::de).toList(),
                categorias.findAllByOrderByOrden().stream().map(CategoriaDto::de).toList(),
                platos.findAllByOrderByCategoriaOrdenAscOrdenAsc().stream().map(PlatoDto::de).toList());
    }

    @PatchMapping("/platos/{id}/disponible")
    @Transactional
    PlatoDto disponible(@PathVariable Long id, @RequestBody DisponibleIn in) {
        Plato p = plato(id);
        p.setDisponible(in.disponible());
        eventos.avisar(Eventos.Tipo.CARTA, p.getId(), null, null);
        return PlatoDto.de(p);
    }

    @PostMapping("/platos")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    PlatoDto crear(@Valid @RequestBody PlatoIn in) {
        Plato p = new Plato(categoria(in.categoriaId()), estacion(in.estacionId()), in.nombre().trim(), in.descripcion(),
                in.precio(), vacioANulo(in.foto()), 99);
        p.setActivo(in.activo());
        platos.save(p);
        eventos.avisar(Eventos.Tipo.CARTA, p.getId(), null, null);
        return PlatoDto.de(p);
    }

    @PutMapping("/platos/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    PlatoDto editar(@PathVariable Long id, @Valid @RequestBody PlatoIn in) {
        Plato p = plato(id);
        p.setCategoria(categoria(in.categoriaId()));
        p.setEstacion(estacion(in.estacionId()));
        p.setNombre(in.nombre().trim());
        p.setDescripcion(in.descripcion());
        p.setPrecio(in.precio());
        p.setFoto(vacioANulo(in.foto()));
        p.setActivo(in.activo());
        eventos.avisar(Eventos.Tipo.CARTA, p.getId(), null, null);
        return PlatoDto.de(p);
    }

    private Plato plato(Long id) {
        return platos.findById(id).orElseThrow(() -> new NoEncontradoException("No existe el plato."));
    }

    private Categoria categoria(Long id) {
        return categorias.findById(id).orElseThrow(() -> new NoEncontradoException("No existe la categoría."));
    }

    private Estacion estacion(Long id) {
        return estaciones.findById(id).orElseThrow(() -> new NoEncontradoException("No existe la estación."));
    }

    private static String vacioANulo(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
