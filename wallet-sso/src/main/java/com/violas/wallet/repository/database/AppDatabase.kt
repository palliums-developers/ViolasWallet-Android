package com.violas.wallet.repository.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.violas.wallet.repository.database.converter.DateConverter
import com.violas.wallet.repository.database.dao.*
import com.violas.wallet.repository.database.entity.*

@Database(
    entities = [AccountDO::class, TokenDo::class, AddressBookDo::class, ApplySSORecordDo::class, SSOApplicationMsgDO::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao
    abstract fun tokenDao(): TokenDao
    abstract fun addressBookDao(): AddressBookDao
    abstract fun applySsoRecordDao(): ApplySSORecordDao
    abstract fun ssoApplicationMsgDao(): SSOApplicationMsgDao

    companion object {
        private const val DATABASE_NAME = "timeDb"
        @Volatile
        private var sInstance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return sInstance ?: synchronized(this) {
                sInstance ?: buildDatabase(context).also { sInstance = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME)
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
//                        val request = OneTimeWorkRequestBuilder<InitDatabaseWorker>().build()
//                        WorkManager.getInstance(context).enqueue(request)
                    }
                })
                .addMigrations(migration1To2())
                .addMigrations(migration2To3())
                .addMigrations(migration3To4())
                .build()
        }

        private fun migration1To2() = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                val sql = """
                CREATE TABLE IF NOT EXISTS apply_sso_record(
	                'id' INTEGER PRIMARY KEY,
	                'account_id' INTEGER NOT NULL,
	                'child_number' INTEGER NOT NULL,
	                'wallet_address' TEXT NOT NULL,
	                'token_address' TEXT NOT NULL,
	                'status' INTEGER NOT NULL
                )
                """
                database.execSQL(sql)
                database.execSQL("CREATE UNIQUE INDEX 'index_apply_sso_record_account_id_child_number' ON apply_sso_record('account_id','child_number')")
            }
        }

        private fun migration2To3() = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("drop table apply_sso_record")
                val sql = """
                CREATE TABLE IF NOT EXISTS apply_sso_record(
	                'id' INTEGER PRIMARY KEY,
	                'child_number' INTEGER NOT NULL,
	                'wallet_address' TEXT NOT NULL,
	                'token_address' TEXT NOT NULL,
	                'status' INTEGER NOT NULL
                )
                """
                database.execSQL(sql)
                database.execSQL("CREATE UNIQUE INDEX 'index_apply_sso_record_wallet_address_child_number' ON apply_sso_record('wallet_address','child_number')")
            }
        }

        private fun migration3To4() = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                val sql = """
                CREATE TABLE IF NOT EXISTS ${SSOApplicationMsgDO.TABLE_NAME}(
	                'id' INTEGER PRIMARY KEY,
	                'account_id' INTEGER NOT NULL,
	                'application_id' TEXT NOT NULL,
	                'application_date' INTEGER NOT NULL,
	                'application_status' INTEGER NOT NULL,
	                'applicant_id_name' TEXT NOT NULL,
	                'issuing_unread' INTEGER NOT NULL,
	                'mint_unread' INTEGER NOT NULL
                )
                """
                database.execSQL(sql)
                database.execSQL("CREATE UNIQUE INDEX 'index_${SSOApplicationMsgDO.TABLE_NAME}_account_id_application_id' ON ${SSOApplicationMsgDO.TABLE_NAME}('account_id','application_id')")
            }
        }
    }
}