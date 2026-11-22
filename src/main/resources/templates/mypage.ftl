<#assign nav = "me">
<#assign tabbar = true>
<#include "_header.ftl">
<section>
  <div class="page-head">
    <div>
      <h1>マイページ</h1>
      <p class="page-sub">${session.displayName} さんの参加履歴と支払記録</p>
    </div>
  </div>

  <div class="stat-cards">
    <div class="stat-card">
      <div class="stat-label">参加したイベント</div>
      <div class="stat-card-value">${events?size}<small>回</small></div>
    </div>
    <div class="stat-card">
      <div class="stat-label">支払合計</div>
      <div class="stat-card-value">${totalText}</div>
    </div>
    <#if nextEvent??>
      <a class="stat-card stat-card-next" href="/g/${guildId?c}/events/${nextEvent.id?c}">
        <span class="stat-label">次の参加予定</span>
        <span class="stat-card-next-text">${nextEvent.dateLabel!""} ${nextEvent.name}</span>
      </a>
    <#else>
      <div class="stat-card">
        <div class="stat-label">次の参加予定</div>
        <div class="stat-card-next-text is-empty">予定はありません</div>
      </div>
    </#if>
  </div>

  <div class="two-col">
    <section class="list-card" aria-labelledby="history-heading">
      <div class="list-card-head">
        <h2 id="history-heading">参加履歴</h2>
        <span class="section-count">新しい順</span>
      </div>
      <#if events?size == 0>
        <p class="list-card-empty">参加したイベントはありません。</p>
      <#else>
        <ul class="list-rows">
          <#list events as event>
            <li>
              <a class="list-row" href="/g/${guildId?c}/events/${event.id?c}">
                <span class="list-row-body">
                  <span class="list-row-sub">第${event.number}回 · ${event.dateLabel!"開催日未定"}</span>
                  <span class="list-row-title">${event.name}</span>
                </span>
                <#if event.isRecruiting>
                  <span class="badge badge-recruit">募集中</span>
                <#else>
                  <span class="badge badge-archive">アーカイブ済</span>
                </#if>
                <svg class="list-row-chevron" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M9 6l6 6-6 6"/></svg>
              </a>
            </li>
          </#list>
        </ul>
      </#if>
    </section>

    <section class="list-card" aria-labelledby="payments-heading">
      <div class="list-card-head">
        <h2 id="payments-heading">支払記録</h2>
        <span class="section-count">幹事が受領を記録した分</span>
      </div>
      <#if payments?size == 0>
        <p class="list-card-empty">支払記録はありません。</p>
      <#else>
        <ul class="list-rows">
          <#list payments as payment>
            <li class="list-row">
              <span class="list-row-body">
                <span class="list-row-title">${payment.eventName}</span>
                <span class="list-row-sub">${payment.shopName} · ${payment.receivedAt}</span>
              </span>
              <span class="list-row-amount">${payment.amountText}</span>
            </li>
          </#list>
        </ul>
        <div class="list-total">
          <span>合計</span>
          <span class="list-total-value">${totalText}</span>
        </div>
      </#if>
    </section>
  </div>
</section>
<#include "_footer.ftl">
