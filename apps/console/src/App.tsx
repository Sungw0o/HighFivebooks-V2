import './App.css'

const services = [
  { name: 'order', role: 'TCC coordinator', status: 'refactor target' },
  { name: 'coupon', role: 'DLQ and idempotency reference', status: 'review target' },
  { name: 'book', role: 'stock hold/restore provider', status: 'runtime dependency' },
  { name: 'member', role: 'grade and points provider', status: 'runtime dependency' },
  { name: 'payment', role: 'payment event publisher', status: 'runtime dependency' },
]

const workflows = [
  'Create order and hold stock',
  'Confirm payment event',
  'Replay poison message',
  'Cancel order and compensate',
]

function App() {
  return (
    <main className="app-shell">
      <aside className="sidebar">
        <div>
          <p className="eyebrow">HighFiveBooks</p>
          <h1>Ops Console</h1>
        </div>
        <nav aria-label="Console sections">
          <a href="#services">Services</a>
          <a href="#workflows">Workflows</a>
          <a href="#kubernetes">Kubernetes</a>
        </nav>
      </aside>

      <section className="workspace">
        <header className="topbar">
          <div>
            <p className="eyebrow">MSA to Kubernetes</p>
            <h2>Refactoring dashboard for order, payment, and messaging flows</h2>
          </div>
          <button type="button">Run Health Check</button>
        </header>

        <section className="summary-grid" aria-label="Project summary">
          <div>
            <span>Primary repo</span>
            <strong>order_server</strong>
          </div>
          <div>
            <span>Frontend</span>
            <strong>React console</strong>
          </div>
          <div>
            <span>Discovery</span>
            <strong>K8s Service DNS</strong>
          </div>
          <div>
            <span>Config</span>
            <strong>ConfigMap / Secret</strong>
          </div>
        </section>

        <section className="panel" id="services">
          <div className="panel-title">
            <div>
              <p className="eyebrow">Service map</p>
              <h3>Runtime services</h3>
            </div>
            <span>5 services</span>
          </div>
          <div className="service-table">
            {services.map((service) => (
              <article key={service.name}>
                <strong>{service.name}</strong>
                <span>{service.role}</span>
                <em>{service.status}</em>
              </article>
            ))}
          </div>
        </section>

        <section className="two-column">
          <div className="panel" id="workflows">
            <div className="panel-title">
              <div>
                <p className="eyebrow">Demo flows</p>
                <h3>Backend evidence</h3>
              </div>
            </div>
            <ul className="workflow-list">
              {workflows.map((workflow) => (
                <li key={workflow}>{workflow}</li>
              ))}
            </ul>
          </div>

          <div className="panel" id="kubernetes">
            <div className="panel-title">
              <div>
                <p className="eyebrow">Kubernetes replacement</p>
                <h3>Spring Cloud exit plan</h3>
              </div>
            </div>
            <dl className="migration-list">
              <div>
                <dt>Eureka</dt>
                <dd>Service DNS</dd>
              </div>
              <div>
                <dt>Config Server</dt>
                <dd>ConfigMap and Secret</dd>
              </div>
              <div>
                <dt>Gateway</dt>
                <dd>Ingress</dd>
              </div>
            </dl>
          </div>
        </section>
      </section>
    </main>
  )
}

export default App
