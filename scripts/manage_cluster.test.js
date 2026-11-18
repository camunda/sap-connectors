// Run with: node --test scripts/
const { describe, it, beforeEach, afterEach, mock } = require('node:test');
const assert = require('node:assert');

const { deleteCluster } = require('./manage_cluster');

const TOKEN = 'test-token';
const CLUSTER_ID = 'cluster-123';
const CLUSTER_URL = `https://api.cloud.ultrawombat.com/clusters/${CLUSTER_ID}`;

class ExitCalled extends Error {
    constructor(code) {
        super(`process.exit(${code})`);
        this.code = code;
    }
}

function response(status, body = '') {
    return {
        ok: status >= 200 && status < 300,
        status,
        text: async () => (typeof body === 'string' ? body : JSON.stringify(body)),
        json: async () => (typeof body === 'string' ? JSON.parse(body) : body)
    };
}

describe('deleteCluster', () => {
    let fetchMock;
    let exitMock;
    let logs;
    let errors;

    // Queues one fetch result per call; an Error instance makes fetch reject.
    function mockFetch(...results) {
        let i = 0;
        fetchMock = mock.method(globalThis, 'fetch', async () => {
            const result = results[i++];
            if (result instanceof Error) throw result;
            return result;
        });
    }

    beforeEach(() => {
        logs = [];
        errors = [];
        mock.method(console, 'log', (...args) => logs.push(args.join(' ')));
        mock.method(console, 'error', (...args) => errors.push(args.join(' ')));
        // fail() calls process.exit(1); throw so execution stops like a real exit
        exitMock = mock.method(process, 'exit', (code) => {
            throw new ExitCalled(code);
        });
    });

    afterEach(() => {
        mock.restoreAll();
    });

    describe('positive flows', () => {
        it('sends an authorized DELETE, then verifies the cluster is gone (404)', async () => {
            mockFetch(response(202), response(404, 'not found'));

            await deleteCluster(TOKEN, CLUSTER_ID);

            assert.strictEqual(fetchMock.mock.callCount(), 2);

            const [deleteUrl, deleteOpts] = fetchMock.mock.calls[0].arguments;
            assert.strictEqual(deleteUrl, CLUSTER_URL);
            assert.strictEqual(deleteOpts.method, 'DELETE');
            assert.strictEqual(deleteOpts.headers.Authorization, `Bearer ${TOKEN}`);

            const [getUrl, getOpts] = fetchMock.mock.calls[1].arguments;
            assert.strictEqual(getUrl, CLUSTER_URL);
            assert.strictEqual(getOpts.method, undefined);
            assert.strictEqual(getOpts.headers.Authorization, `Bearer ${TOKEN}`);

            assert.strictEqual(exitMock.mock.callCount(), 0);
            assert.ok(logs.includes(`[OK] Cluster deletion request sent for id=${CLUSTER_ID}`));
            assert.ok(logs.includes(`[INFO] Cluster completely deleted by ID: ${CLUSTER_ID}`));
            assert.ok(!logs.some((l) => l.startsWith('[WARN]')));
        });

        it('warns but does not fail when the post-deletion status check returns a non-404 error', async () => {
            mockFetch(response(200), response(500, 'boom'));

            await deleteCluster(TOKEN, CLUSTER_ID);

            assert.strictEqual(exitMock.mock.callCount(), 0);
            assert.ok(logs.includes(`[WARN] Cluster (${CLUSTER_ID}) deletion status undefined!`));
            assert.ok(!logs.some((l) => l.includes('completely deleted')));
        });
    });

    describe('negative flows', () => {
        it('fails when the DELETE request is rejected by the API', async () => {
            mockFetch(response(403, 'forbidden'));

            await assert.rejects(deleteCluster(TOKEN, CLUSTER_ID), ExitCalled);

            assert.strictEqual(fetchMock.mock.callCount(), 1);
            assert.deepStrictEqual(exitMock.mock.calls.map((c) => c.arguments[0]), [1, 1]);
            assert.ok(errors.some((e) => e.includes('Cluster deletion failed (403): forbidden')));
        });

        it('fails when the cluster still exists after deletion', async () => {
            mockFetch(response(202), response(200, { status: 'Healthy' }));

            await assert.rejects(deleteCluster(TOKEN, CLUSTER_ID), ExitCalled);

            assert.strictEqual(fetchMock.mock.callCount(), 2);
            assert.strictEqual(exitMock.mock.calls[0].arguments[0], 1);
            assert.ok(errors.some((e) => e.includes('after deletion!!!')));
            assert.ok(!logs.some((l) => l.includes('completely deleted')));
        });

        it('fails when the DELETE request throws a network error', async () => {
            mockFetch(new Error('ECONNRESET'));

            await assert.rejects(deleteCluster(TOKEN, CLUSTER_ID), ExitCalled);

            assert.strictEqual(fetchMock.mock.callCount(), 1);
            assert.ok(errors.some((e) => e.includes('Failed to delete cluster: ECONNRESET')));
        });

        it('fails when the verification request throws a network error', async () => {
            mockFetch(response(202), new Error('ETIMEDOUT'));

            await assert.rejects(deleteCluster(TOKEN, CLUSTER_ID), ExitCalled);

            assert.strictEqual(fetchMock.mock.callCount(), 2);
            assert.ok(errors.some((e) => e.includes('Failed to delete cluster: ETIMEDOUT')));
        });
    });
});
