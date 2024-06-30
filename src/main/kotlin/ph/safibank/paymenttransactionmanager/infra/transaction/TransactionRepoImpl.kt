package ph.safibank.paymenttransactionmanager.infra.transaction

import io.micronaut.core.annotation.Indexed
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.GenericRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import jakarta.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ph.safibank.common.utils.GlobalUtils.getLogger
import ph.safibank.paymenttransactionmanager.core.transaction.Transaction
import ph.safibank.paymenttransactionmanager.core.transaction.TransactionRepo
import java.util.UUID
import javax.transaction.Transactional

@Singleton
@Transactional
class TransactionRepoImpl(
    private val repo: TransactionDbRepo,
) : TransactionRepo {

    private val log = getLogger()

    override suspend fun saveTransaction(transaction: Transaction): Transaction = withContext(Dispatchers.IO) {
        val entity = transaction.toEntity()
        log.info("Saving entity with id ${entity.id} : {} ", entity)
        val savedEntity = repo.save(entity)
        savedEntity.toModel()
    }

    override suspend fun updateTransaction(transaction: Transaction): Transaction = withContext(Dispatchers.IO) {
        val entity = transaction.toEntity()
        log.info("Updating entity with id ${entity.id} : {} ", entity)
        val savedEntity = repo.update(entity)
        savedEntity.toModel()
    }

    override suspend fun getTransaction(id: UUID): Transaction? = withContext(Dispatchers.IO) {
        val transactionEntity = repo.findById(id)
        log.info("Transaction found: {}", transactionEntity)
        transactionEntity?.toModel()
    }
}

@JdbcRepository(dialect = Dialect.POSTGRES)
@Transactional(Transactional.TxType.MANDATORY)
@Indexed(GenericRepository::class)
interface TransactionDbRepo : CoroutineCrudRepository<TransactionEntity, UUID>
