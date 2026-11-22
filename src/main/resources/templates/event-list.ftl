<#assign nav = "events">
<#assign tabbar = true>
<#include "_header.ftl">
<section>
  <div class="page-head">
    <div>
      <h1>イベント</h1>
      <p class="page-sub">参加したいイベントを選んで、参加ボタンを押してください。</p>
    </div>
    <a class="button" href="/g/${guildId?c}/events/new">
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" aria-hidden="true"><path d="M12 5v14M5 12h14"/></svg>
      イベントを作成
    </a>
  </div>

  <nav class="segmented page-tabs" aria-label="イベントの状態">
    <a class="segmented-item" href="/g/${guildId?c}/events?status=recruiting"<#if status == 'recruiting'> aria-current="page"</#if>>募集中</a>
    <a class="segmented-item" href="/g/${guildId?c}/events?status=archived"<#if status == 'archived'> aria-current="page"</#if>>アーカイブ</a>
  </nav>

  <#if events?size == 0>
    <p class="empty">イベントがありません。</p>
  <#else>
    <ul class="event-list">
      <#list events as event>
        <li>
          <a class="event-row<#if !event.isRecruiting> is-archived</#if>" href="/g/${guildId?c}/events/${event.id?c}">
            <span class="date-block" aria-hidden="true">
              <#if event.dateDay??>
                <span class="date-block-month">${event.dateMonth}月</span>
                <span class="date-block-day">${event.dateDay}</span>
                <span class="date-block-dow">${event.dateWeekday}</span>
              <#else>
                <span class="date-block-tbd">未定</span>
              </#if>
            </span>
            <span class="event-row-body">
              <span class="event-row-number">第${event.number}回<#if !event.isRecruiting> <span class="badge badge-archive">アーカイブ済</span></#if></span>
              <span class="event-row-name">${event.name}</span>
              <span class="event-row-meta">
                <span class="sr-only">開催日 ${event.dateLabel!"未定"}</span>
                <span class="meta-item">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M12 21s-7-6.2-7-11a7 7 0 0 1 14 0c0 4.8-7 11-7 11z"/><circle cx="12" cy="10" r="2.5"/></svg>
                  ${event.area}
                </span>
                <span class="meta-item">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="12" cy="8" r="4"/><path d="M4 21a8 8 0 0 1 16 0"/></svg>
                  幹事 ${event.organizerName}
                </span>
                <span class="meta-item meta-count">${event.participantCount!""}</span>
              </span>
            </span>
            <span class="event-row-count">
              <span class="event-row-count-label">参加</span>
              <span class="event-row-count-value">${event.participantCount!""}</span>
            </span>
            <svg class="event-row-chevron" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M9 6l6 6-6 6"/></svg>
          </a>
        </li>
      </#list>
    </ul>
  </#if>
</section>
<#include "_footer.ftl">
