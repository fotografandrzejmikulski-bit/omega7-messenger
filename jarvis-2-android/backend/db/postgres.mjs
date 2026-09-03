import pg from 'pg';
const {Pool}=pg;
export function createPostgres(){if(!process.env.DATABASE_URL)return null;const pool=new Pool({connectionString:process.env.DATABASE_URL,ssl:process.env.DATABASE_SSL==='false'?false:{rejectUnauthorized:false},max:Number(process.env.DATABASE_POOL_MAX||10)});return pool}
export async function vectorSearch(pool,embedding,limit=8){const r=await pool.query('SELECT id,text,metadata,1-(embedding<=>$1::vector) AS score FROM jarvis_memory WHERE embedding IS NOT NULL ORDER BY embedding<=>$1::vector LIMIT $2',[JSON.stringify(embedding),limit]);return r.rows}
