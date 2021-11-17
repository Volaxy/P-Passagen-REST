package br.com.etaure.resource;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.google.gson.Gson;

import br.com.etaure.dao.PassageiroDAO;
import br.com.etaure.dao.PassagemDAO;
import br.com.etaure.entities.Passageiro;
import br.com.etaure.entities.Passagem;
import br.com.etaure.entities.dto.PassagemComIdPassageiroDTO;

@Path("passagens")
public class PassagemResource {

	private ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
	private Validator validator = factory.getValidator();

	// Lista todas as Passagens
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response listAll() {
		List<Passagem> passagens = PassagemDAO.findAll();

		if (passagens.isEmpty()) {
			return Response.status(Status.NO_CONTENT).build();
		} else {
			// Caso tenha ao menos 1 passagem, retorna todas as passagens registradas em formato Json
			StringBuilder sb = new StringBuilder();

			for (Passagem passagem : passagens) {
				sb.append(passagem.toJson());
			}

			return Response.ok(sb.toString()).build();
		}
	}

	// Lista a Passagem com o id passado por par�metro
	@GET
	@Path("{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response findById(@PathParam("id") Integer id) {
		Passagem passagem = PassagemDAO.findById(id);

		if (passagem == null) {
			return Response.status(Status.NOT_FOUND).build();
		} else {
			return Response.ok(passagem.toJson()).build();
		}
	}

	// Insere uma nova Passagem para o banco de dados
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	public Response add(String passagemJson) throws URISyntaxException {
		// Transforma os par�metros passados em formato Json para um novo objeto PassagemCriadaDTO que cont�m os valores da passagem e somente o id do passageiro
		PassagemComIdPassageiroDTO passagemComIdPassageiroDTO = new Gson().fromJson(passagemJson, PassagemComIdPassageiroDTO.class);
		
		// Se contiver dados inv�lidos, informa ao usu�rio mandando um erro 400
		Passagem passagem = null;
		try {
			passagem = passagemComIdPassageiroDTO.retornarObjetoPassagem();
		} catch(IllegalArgumentException e) {
			return Response.ok(e.getMessage()).status(Status.BAD_REQUEST).build(); 
		}
		
		// Procura pelo id do passageiro no banco
		Passageiro passageiro = PassageiroDAO.findById(passagemComIdPassageiroDTO.getIdPassageiro());
		
		// Se contiver o passageiro com o id especificado no banco, atribui ao objeto Passagem
		if(passageiro == null) {
			return Response.ok("Id do passageiro n�o encontrado no banco").status(Status.NOT_FOUND).build();
		} else {
			passagem.setPassageiro(passageiro);
		}
		
		// Verifica se os campos s�o v�lidos
		Set<ConstraintViolation<Passagem>> violations = validator.validate(passagem);

		// Caso contenha ao menos 1 erro, retorna as mensagens de erro de par�metro para o usu�rio, caso n�o tenha, insere uma nova passagem no banco
		if (violations.isEmpty()) {
			PassagemDAO.insert(passagem);

			// Prepara a URI para redirecionar o cliente para a nova passagem
			URI uri = new URI("passagens/" + passagem.getId());

			return Response.created(uri).status(Status.CREATED).build();
		} else {
			StringBuilder sb = new StringBuilder();
			for (ConstraintViolation<Passagem> violation : violations) {
				sb.append(violation.getMessage());
			}

			return Response.ok(sb.toString()).status(Status.NOT_FOUND).build();
		}
	}

	// Atualiza uma Passagem que j� existe no banco de acordo com o "id" passado
	// como par�metro pela requisi��o HTTP
	@PUT
	@Path("{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response update(@PathParam("id") Integer id, String passagemJson) throws URISyntaxException {
		PassagemComIdPassageiroDTO passagemTemporaria = new Gson().fromJson(passagemJson, PassagemComIdPassageiroDTO.class);
		
		Passagem newPassagem = null;
		// Cria uma nova passagem sem o Passageiro
		try {
			newPassagem = passagemTemporaria.retornarObjetoPassagem();
		} catch(IllegalArgumentException e) {
			return Response.ok(e.getMessage()).status(Status.BAD_REQUEST).build();
		}
		
		// Verifica se existe a Passagem com o id passado por par�metro no banco,
		// e o Passageiro passado no corpo da requisi��o
		Passagem oldPassagem = PassagemDAO.findById(id);
		Passageiro passageiro = PassageiroDAO.findById(passagemTemporaria.getIdPassageiro());
		
		if (oldPassagem == null || passageiro == null) {
			return Response.status(Status.NOT_FOUND).build();
		} else {
			newPassagem.setPassageiro(passageiro);
			
			// Verifica se os campos s�o v�lidos
			Set<ConstraintViolation<Passagem>> violations = validator.validate(newPassagem);

			// Atualiza a passagem antiga com os dados da nova Passagem
			if(violations.isEmpty()) {
				PassagemDAO.update(id, newPassagem);

				return Response.ok().build();
			} else {
				StringBuilder sb = new StringBuilder();
				for (ConstraintViolation<Passagem> violation : violations) {
					sb.append(violation.getMessage());
				}

				// Retorna uma resposta HTTP com todos os requisitos que n�o foram cumpridos
				return Response.ok(sb.toString()).status(Status.NOT_FOUND).build();
			}
		
		}
	}

	// Deleta uma passagem do banco
	@DELETE
	@Path("{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response delete(@PathParam("id") Integer id) {
		Passagem passagem = PassagemDAO.findById(id);

		if (passagem == null) {
			return Response.status(Status.NOT_FOUND).build();
		} else {
			PassagemDAO.delete(id);

			return Response.ok().build();
		}
	}

}
