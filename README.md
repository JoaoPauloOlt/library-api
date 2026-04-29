# 📚 Library API

API RESTful para gerenciamento de biblioteca, desenvolvida com **Spring Boot**, seguindo boas práticas de arquitetura e segurança com **JWT**.

---

## 🚀 Tecnologias

* Java 17+
* Spring Boot
* Spring Security
* JWT (JSON Web Token)
* Spring Data JPA
* MySQL
* Maven

---

## 🔐 Segurança

A API utiliza autenticação baseada em **JWT**:

* Login via `/auth/login`
* Token deve ser enviado no header:

```
Authorization: Bearer {token}
```

---

## 📦 Funcionalidades

### 👤 Usuários

* Criar usuário
* Listar usuários

### ✍️ Autores

* Criar, listar, buscar, atualizar e deletar autores

### 📚 Livros

* CRUD completo
* Filtros dinâmicos:

    * título
    * gênero
    * autor
    * disponibilidade

### 🔄 Empréstimos

* Realizar empréstimo
* Devolver livro
* Listar empréstimos

---

## 🧠 Arquitetura

O projeto segue uma arquitetura em camadas:

```
api (controllers, DTOs, assemblers)
domain (model, service, repository, exceptions)
infrastructure (security, config)
```

---

## ⚙️ Configuração

### 🔹 Profiles

O projeto utiliza profiles:

* `dev` → ambiente local
* `prod` → produção

---

### 🔹 Variáveis de ambiente (produção)

```env
DB_URL=
DB_USER=
DB_PASS=
JWT_SECRET=
```

---

## ▶️ Como rodar o projeto

### 1. Clonar repositório

```bash
git clone https://github.com/JoaoPauloOlt/library-api.git
```

### 2. Configurar banco de dados

Crie um banco MySQL chamado:

```
library
```

---

### 3. Rodar aplicação

```bash
./mvnw spring-boot:run
```

---

## 🧪 Testes da API

Você pode testar usando:

* Postman
* Insomnia

---

### 🔑 Exemplo de login

```json
POST /auth/login

{
  "email": "user@email.com",
  "password": "123"
}
```

---

## 🌐 CORS

A API está configurada para aceitar requisições de front-end (React/Vite).

---

## 📌 Próximos passos

* Deploy no Render
* Integração com front-end (React)
* Documentação com Swagger

---

## 👨‍💻 Autor

Desenvolvido por **João Paulo Oltramari**.
