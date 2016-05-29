package com.github.rmannibucau.rblog.security.web;

import com.github.rmannibucau.rblog.jpa.Token;
import com.github.rmannibucau.rblog.jpa.User;
import com.github.rmannibucau.rblog.security.TokenPrincipal;
import com.github.rmannibucau.rblog.security.cdi.Logged;
import com.github.rmannibucau.rblog.security.service.TokenGenerator;
import com.github.rmannibucau.rblog.security.web.model.Credentials;
import com.github.rmannibucau.rblog.security.web.model.TokenValue;
import com.github.rmannibucau.rblog.service.PasswordService;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import javax.ws.rs.Consumes;
import javax.ws.rs.HEAD;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import java.util.Set;

import static com.github.rmannibucau.rblog.lang.Exceptions.orNull;
import static java.util.Collections.emptySet;
import static java.util.Optional.ofNullable;

@Transactional
@Path("security")
@ApplicationScoped
public class TokenResource {
    @Inject
    private TokenGenerator tokenGenerator;

    @Inject
    private PasswordService passwordService;

    @Inject
    private EntityManager entityManager;

    @POST
    @Path("login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public TokenValue login(final Credentials credentials) {
        final User user = orNull(
            () -> entityManager.createNamedQuery(User.FIND_BY_CREDENTIALS, User.class)
                .setParameter("username", credentials.getUsername())
                .setParameter("password", passwordService.toDatabaseFormat(credentials.getPassword()))
                .getSingleResult(),
            NoResultException.class);
        if (!ofNullable(user).isPresent()) {
            throw new InvalidCredentialsException();
        }

        final String tokenValue = tokenGenerator.next();

        final Token token = new Token();
        token.setValue(tokenValue);
        token.setUser(user);
        entityManager.persist(token);
        entityManager.flush();

        return new TokenValue(tokenValue);
    }

    @HEAD
    @Logged
    @Path("logout")
    public void logout(@Context final HttpServletRequest request) {
        entityManager.remove(entityManager.getReference(Token.class, TokenPrincipal.class.cast(request.getUserPrincipal()).getToken()));
    }

    @HEAD
    @Logged
    @Path("empty")
    public void empty(@Context final HttpServletRequest request) {
        final Token current = entityManager.getReference(Token.class, TokenPrincipal.class.cast(request.getUserPrincipal()).getToken());
        final Set<Token> tokens = ofNullable(current.getUser().getTokens()).orElse(emptySet());
        tokens.stream().filter(t -> !t.equals(current)).forEach(entityManager::remove);
        if (!tokens.isEmpty()) {
            entityManager.flush();
        }

    }
}
