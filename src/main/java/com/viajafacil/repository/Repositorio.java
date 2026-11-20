package com.viajafacil.repository;

import java.util.List;

/** Contrato de persistência. Hoje é JSON em arquivo; poderia ser CSV, banco ou Google Drive sem mudar os serviços. */
public interface Repositorio<T> {
    List<T> carregar();

    void salvar(List<T> itens);
}
